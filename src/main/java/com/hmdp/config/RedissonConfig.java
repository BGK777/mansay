package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RedissonConfig {

    @Value("${redisconf.url}")
    String redisUrl;

    @Bean
    public RedissonClient redissonClient(){
        //配置
        Config config = new Config();
        //虚拟机位置，密码
        config.useSingleServer().setAddress(redisUrl).setPassword("BGK796288311");
        //创建RedissonClient对象
         return Redisson.create(config);
    }
}
