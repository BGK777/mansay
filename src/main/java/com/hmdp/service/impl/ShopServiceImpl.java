package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.systemUtil.CacheClient;
import com.hmdp.utils.enumUtil.SystemConstants;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.springframework.data.domain.Example;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.enumUtil.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Redis工具包，解决缓存击穿，缓存穿透问题
     */
    @Resource
    private CacheClient cacheClient;

    /**
     * 根据店铺id查询店铺缓存
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        // 解决缓存穿透
//        Shop shop = cacheClient
//                .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

//        互斥锁解决缓存击穿
         Shop shop = cacheClient
                 .queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 逻辑过期解决缓存击穿
//         Shop shop = cacheClient
//                 .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        // 7.返回
        return Result.ok(shop);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result update(Shop shop) {
        //获取店铺id，并判断是否存在
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空!");
        }
        //先更新数据库
        updateById(shop);
        //再删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current) {
        LambdaQueryWrapper<Shop> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Shop::getTypeId,typeId);
        IPage<Shop> page = page(new Page<>(current,SystemConstants.MAX_PAGE_SIZE),lqw);
        return Result.ok(page.getRecords());
    }
}
