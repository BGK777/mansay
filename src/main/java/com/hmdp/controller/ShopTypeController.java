package com.hmdp.controller;


import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.enumUtil.RedisConstants.CACHE_SHOPTYPE_TTL;
import static com.hmdp.utils.enumUtil.RedisConstants.CACHE_SHOP_TYPE;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    public Result queryTypeList() {
        //在Redis中查询商铺类型类表
        String shopTypeListJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE);
        //JSONString转化为List
        List<ShopType> shopTypeList = JSONUtil.toList(shopTypeListJson, ShopType.class);
        //判断是否为空
        if(!shopTypeList.isEmpty()){
            //不为空返回shopTypeList
            return Result.ok(shopTypeList);
        }
        //为空,去数据库查找
        shopTypeList = typeService.list();
        //是否为存在
        if(shopTypeList.isEmpty()){
            //不存在，返回系统错误
            return Result.fail("系统错误");
        }
        //存在，写入数据到Redis，且返回
        String toJsonStr = JSONUtil.toJsonStr(shopTypeList);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE,toJsonStr,CACHE_SHOPTYPE_TTL, TimeUnit.MINUTES);
        return Result.ok(shopTypeList);
    }
}
