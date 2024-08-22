package com.dianping.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.dto.Result;
import com.dianping.entity.Shop;
import com.dianping.mapper.ShopMapper;
import com.dianping.rabbitmq.MQSender;
import com.dianping.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.utils.BloomFilterService;
import com.dianping.utils.RedisData;
import com.dianping.utils.SystemConstants;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.dianping.utils.RedisConstants.*;

/**
 * @description: 商铺信息 ServiceImpl
 * @author Wangyw
 * @date 2024/5/17 19:33
 * @version 1.0
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private MQSender mqSender;
    @Resource
    private BloomFilterService bloomFilterService;
    @Resource
    private RedissonClient redissonClient;

    // 线程池，用于异步缓存重建
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(20);


    // 查询商铺数据，使用布隆过滤器防止缓存穿透问题
    // 布隆过滤器在单元测试中进行的初始化
    @Override
    public Result queryById(Long id) {
        // 0. 先查询布隆过滤器，如果不存在，直接返回错误信息
        if (!bloomFilterService.containsElement(id)) {
            return Result.fail("店铺信息不存在，请确认后再查询。");
        }
        // 1. 从redis查询商铺缓存
        String key = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断缓存是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3. 存在，直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        // 4. 不存在，查数据库
        Shop shop = getById(id);
        // 5. 数据库中也不存在
        if (shop == null) {
            return Result.fail("店铺信息不存在");
        }
        // 6. 数据库存在，写入redis
        // 写入过期时间，保证最终一致性
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 7. 返回商铺数据
        return Result.ok(shop);
    }


    // 使用redisson互斥锁 + 逻辑过期 解决热点key缓存击穿问题
    public Result queryHotShopById(Long id) {
        // 0. 先查询布隆过滤器，如果不存在，直接返回错误信息
        if (!bloomFilterService.containsElement(id)) {
            return Result.fail("店铺信息不存在，请确认后再查询。");
        }
        // 1. 从redis查询商铺缓存
        String key = CACHE_HOT_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断缓存是否存在
        // 热点key要进行预热，保证缓存一定存在
        if (StrUtil.isBlank(shopJson)) {
//            saveShop2Redis(id, 20L);
            return Result.fail("缓存不存在");
        }

        // 4. 存在，判断缓存是否过期
        // json反序列化
        RedisData shopData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) shopData.getData(), Shop.class);
        LocalDateTime expireTime = shopData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 未过期，返回店铺信息
            return Result.ok(shop);
        }
        // 过期，异步缓存重建

        // 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        RLock myLock = redissonClient.getLock(lockKey);

        // 创建一个线程可父子传递的ThreadLocal，放父线程id
        final TransmittableThreadLocal<Long> transmittableThreadLocal = new TransmittableThreadLocal<>();
        long threadId = Thread.currentThread().getId();
        transmittableThreadLocal.set(threadId);

        boolean isLock = myLock.tryLock();
        // 获取锁失败，直接返回店铺信息。
//        if (!isLock) {
//            log.debug("获取锁失败");
//        }

        // 获取锁成功
        if (isLock) {
            try {
                // double check
                // 1. 从redis查询商铺缓存
                shopJson = stringRedisTemplate.opsForValue().get(key);
                // 2. 判断缓存是否存在
                if (StrUtil.isBlank(shopJson)) {
                    // 3. 不存在，直接返回null
                    return Result.fail("缓存不存在");
                }

                // 4. 存在，判断缓存是否过期
                // json反序列化
                shopData = JSONUtil.toBean(shopJson, RedisData.class);
                shop = JSONUtil.toBean((JSONObject) shopData.getData(), Shop.class);
                expireTime = shopData.getExpireTime();
                if (expireTime.isAfter(LocalDateTime.now())) {
                    // 未过期，返回店铺信息
                    return Result.ok(shop);
                }

                // 成功之后，开启独立线程，进行缓存重建
                // 在子线程中释放父线程加的锁
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    log.debug("重建缓存开始");
                    try {
//                        Thread.sleep(60000);
                        this.saveShop2Redis(id, 20L);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        // 使用 threadId 也可以，因为在一块，可以上下文传值
                        myLock.unlockAsync(transmittableThreadLocal.get());
                    }
                    log.debug("缓存重建完成");
                });

            } catch (Exception e) {
                throw new RuntimeException(e);
            }finally {
                // 移除threadLocal对象，谨防内存泄漏！！！
                transmittableThreadLocal.remove();
                log.debug("remove方法完成");
            }
        }
        // 7. 返回商铺数据
        return Result.ok(shop);
    }


    // 热点key预热，提前保存热点key到redis
    private void saveShop2Redis(Long id, Long expireSeconds) {
        Shop shop = getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_HOT_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }


    // 更新商铺信息，更新到数据库，同时要保证缓存的最终一致性。
    @Override
    public Result update(Shop shop) {
        // 1. 判断shop不为空
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("商铺id不能为空");
        }
        // 2. 更新数据库
        updateById(shop);
        // 3. 删除缓存
        String key = CACHE_SHOP_KEY + id;
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            // 删除失败后，放到消息队列中
            mqSender.sendCacheDelMessage(key);
            log.error(e.getMessage());
        }

        return Result.ok();
    }

}
