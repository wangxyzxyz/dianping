package com.dianping.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @description: 基于 Redis 生成全局唯一id
 * @author Wangyw 
 * @date 2024/5/18 21:25
 * @version 1.0
 */
@Component
public class RedisIdWorker {

    // 开始时间戳(2024/1/1 0.0.0)
    private static final long START_TIMESTAMP = 1704067200L;

    // 序列号的位数
    private static final int COUNT_BITS = 32;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Redis 生成全局唯一id
    public long nextId(String prefix) {
        // 获取时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSecond - START_TIMESTAMP;

        // 获取当前日期（某一天）
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 得到序列号，根据redis自增长
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + prefix + ":" + date);
        return timeStamp  << COUNT_BITS | count;
    }

}
