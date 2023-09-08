package com.hmdp.utils.lookUtil;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock {

    private final String name;
    private final StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    //初始化Lua脚本
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("luacof//unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    /**
     * 获取锁
     *
     * @param timeOutSec
     * @return
     */
    public boolean tryLock(long timeOutSec) {
        //获取线程标示
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate
                .opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeOutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

        /**
         * 释放锁
         */
        public void unlock () {
            //调用Lua脚本
            stringRedisTemplate.execute(
                    UNLOCK_SCRIPT,
                    Collections.singletonList(KEY_PREFIX+name),
                    ID_PREFIX + Thread.currentThread().getId()
            );
        }
    }
