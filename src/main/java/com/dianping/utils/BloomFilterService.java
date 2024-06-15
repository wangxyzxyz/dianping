package com.dianping.utils;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @description: 布隆过滤器
 * @author Wangyw
 * @version 1.0
 * @date 2024/5/18 15:41
 */
@Service
public class BloomFilterService {
    @Resource
    private RedissonClient redissonClient;

    private RBloomFilter<Long> bloomFilter;

    // 执行顺序：Constructor(构造方法) -> @Autowired(依赖注入) -> @PostConstruct(注释的初始化方法)
    @PostConstruct
    public void init() {
        // 初始化布隆过滤器
        bloomFilter = redissonClient.getBloomFilter("shopBloomFilter");
        // 配置布隆过滤器的预期插入数量和误差率
        bloomFilter.tryInit(10000, 0.01);
    }

    // 向布隆过滤器添加元素
    public void add(Long element) {
        bloomFilter.add(element);
    }

    // 检查元素是否存在于布隆过滤器
    public boolean containsElement(Long element) {
        return bloomFilter.contains(element);
    }
}
