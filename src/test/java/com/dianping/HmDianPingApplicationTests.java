package com.dianping;

import com.dianping.entity.Shop;
import com.dianping.service.IShopService;

import com.dianping.utils.BloomFilterService;
import com.dianping.utils.RedisIdWorker;
import com.dianping.utils.UserUtil;
import com.sankuai.inf.leaf.service.SegmentService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.dianping.utils.RedisConstants.SHOP_GEO_KEY;

// websocket导致单元测试失败，测试的时候注掉websocket
@Slf4j
@SpringBootTest()
class HmDianPingApplicationTests {
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private IShopService shopService;

    @Resource
    private BloomFilterService bloomFilterService;

    @Resource
    private SegmentService segmentService;

    @Resource
    private UserUtil userUtil;


    // 生成登录用户token信息，用于秒杀压测
    @Test
    void genFile() throws Exception {
        userUtil.createUser(5000);
    }

    // leaf生成id测试
    @Test
    void contextLoads() {
        for (int i= 0; i < 1000; i++) {
            long id1 = segmentService.getId("seckill").getId();
            log.info("id:{}", id1);
        }
    }

    private ExecutorService es = Executors.newFixedThreadPool(500);

    // 测试 RedisIdWorker 工具类
    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id:" + id);
            }
            latch.countDown();
        };
        long beginTime = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        System.out.println("time = " + (endTime - beginTime));
    }


    // 预热布隆过滤器中元素
    @Test
    void addElementToBloom() {
        List<Shop> list = shopService.list();
        for (Shop shop : list) {
            bloomFilterService.add(shop.getId());
        }
    }


    // 测试布隆过滤器工作是否正常
    @Test
    void isContains() {
        Long n1 = 5L;
        Long n2 = 50L;
        System.out.println(bloomFilterService.containsElement(n1));
        System.out.println(bloomFilterService.containsElement(n2));
    }

}
