package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.systemUtil.RedisIdWorker;
import com.hmdp.utils.systemUtil.ThreadPollUtil;
import com.hmdp.utils.systemUtil.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    //初始化Lua脚本,设置脚本路径和返回值类型
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("luacof/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @PostConstruct
    private void init() {
        //使用线程池工具类中的单例实例
        ThreadPollUtil.getInstance().poolExecutor.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable {
        String queueName = "stream.orders";

        /**
         * 循环获取消息队列消息并处理，如果发生异常，交给异常处理handlePendingList
         */
        @Override
        public void run() {
            while (true) {
                try {
                    //1.获取消息队列中的订单信息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    //2.判断获取消息是否成功
                    if (list == null || list.isEmpty()) {
                        //2.1失败说明没有消息，继续下一次循环
                        continue;
                    }
                    orderAndACK(list);
                } catch (Exception e) {
                    handlePendingList();
                }
            }
        }

        /**
         * 解析消息队列消息并执行下单，ACK消息消费确定
         * @param list
         */
        private void orderAndACK(List<MapRecord<String, Object, Object>> list) {
            // 解析数据
            MapRecord<String, Object, Object> record = list.get(0);
            Map<Object, Object> value = record.getValue();
            VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
            //3.成功，可以下单
            createVoucherOrder(voucherOrder);
            //ACK确认
            stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
        }

        /**
         * 处理异常消息，还异常的话，等待后再次循环处理
         */
        private void handlePendingList() {
            while (true) {
                try {
                    // 1.获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    // 2.判断订单信息是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有异常消息，结束循环
                        break;
                    }
                    // 解析数据
                    orderAndACK(list);
                } catch (Exception e) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

    /**
     * 优惠券秒杀（lua+消息队列）
     * @param voucherId
     * @return
     */
    @Override
    public Result secKillVoucher(Long voucherId) {
        //1.执行lua脚本
        //1.1获取用户id
        //订单id
        long orderId = redisIdWorker.nextId("order");
        Long userId = UserHolder.getThreadLocal().get().getId();
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId));
        //2.判断结果是否为0
        assert result != null;
        int r = result.intValue();
        if (r != 0) {
            //2.1不为0 。代表没有下单成功
            return Result.fail(r == 1 ? "库存不足!" : "不能重复下单!");
        }
        //3.返回订单id
        return Result.ok(orderId);
    }


    /**
     * 消费消息队列信息后，调用此方法创建订单 rollbackFor = Exception.class作用是运行时异常和非运行时异常都会发生回滚
     * @param voucherOrder
     */
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        // 创建锁对象（兜底）
        RLock redisLock = redissonClient.getLock("lock:order:" + userId);
        // 尝试获取锁
        boolean isLock = redisLock.tryLock();
        // 判断
        if (!isLock) {
            // 获取锁失败，直接返回失败或者重试
            log.error("发生未知错误！");
            return;
        }
        try{
            //扣减库存
            boolean isSuccess = seckillVoucherService.update(
                    new LambdaUpdateWrapper<SeckillVoucher>()
                            .eq(SeckillVoucher::getVoucherId, voucherOrder.getVoucherId())
                            // 条件库存必须大于0，防止超卖
                            .gt(SeckillVoucher::getStock, 0)
                            .setSql("stock=stock-1"));
            // 创建订单
            save(voucherOrder);
        } finally {
            // 释放锁
            redisLock.unlock();
        }
    }
}
