package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 全局唯一ID生成器
 */
@Component
public class RedisIdWorker {
    /**
     * 开始时间戳，2022年1月1日0时0分0秒
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    private static final int BEGIN_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 序列号的位数
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix) {
        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;

        //2.生成序列号
        //2.1 获取今天年月日
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //2.2 设置Redis自增
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        //3.拼接并返回
        return timeStamp << BEGIN_BITS | count;
    }
}
