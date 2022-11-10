package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.hmdp.config.RedissonConfig;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService iSeckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Result secKillVoucher(Long voucherId) {
        //1.查询优惠券信息
        SeckillVoucher seckillVoucher = iSeckillVoucherService.getById(voucherId);
        //2.判断秒杀活动是否开始,或是否已经结束
        if(seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            //是否开始,否，直接返回结果异常
            return Result.fail("秒杀活动未开始！");

        }
        if(seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            //是否已经结束,是，直接返回结果异常
            return Result.fail("秒杀活动已经结束！");
        }
        //3.是，判断库存是否充足
        Integer stock = seckillVoucher.getStock();
        if(stock < 1){
            //不充足，返回结果异常
            return Result.fail("库存不足！");
        }
//        //4.一人一单
        Long userId = UserHolder.getUser().getId();
//        //悲观锁
//        synchronized (userId.toString().intern()){
//            //获取代理对象（事务）
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId,userId);
//        }
        //分布式锁
        //获取锁对象
//        SimpleRedisLock lock = new SimpleRedisLock("order:"+userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //获取锁
        boolean isLock = lock.tryLock();
        if(!isLock){
            //获取失败，返回错误
            return Result.fail("不允许重复下单");
        }
        try {
            //获取代理对象（事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
    }


    /**
     * 创建优惠券订单
     * @param voucherId
     * @return
     */
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        //4.1 查询订单
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //4.2 判断是否存在
        if (count > 0) {
            //用户已经购买过，不可再购买，返回失败！
            return Result.fail("用户已经购买过，不可再购买!");
        }
        //5.充足，减扣库存,施加乐观锁，修改时还要判断库存剩余是否大于0
        boolean success = iSeckillVoucherService.update()
                .setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock", 0).update();
        if (!success) {
            //不充足，返回结果异常
            return Result.fail("库存不足！");
        }
        //6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //6.1 订单id
        long orderId = redisIdWorker.nextId("secKillVoucher");
        voucherOrder.setId(orderId);
        //6.2 用户id
        voucherOrder.setUserId(userId);
        //6.3 代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(orderId);
    }
}
