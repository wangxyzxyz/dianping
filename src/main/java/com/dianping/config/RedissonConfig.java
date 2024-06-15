package com.dianping.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: Redisson 配置
 * @author Wangyw
 * @date 2024/6/3 0:24
 * @version 1.0
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://xxx:6379")
                .setPassword("xxx");
        return Redisson.create(config);
    }


}
