package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient  redissonClient(){
        //配置
        Config config = new Config();
        //虚拟机位置，密码
        config.useSingleServer().setAddress("redis://192.168.111.100:6379").setPassword("BGK796288311");
        //创建RedissonClient对象
         return Redisson.create(config);
    }
}
