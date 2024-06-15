package com.dianping.utils;

/**
 * @description:  RabbitMQ 中常数定义
 * @author Wangyw
 * @date 2024/6/3 14:57
 * @version 1.0
 */
public class MQConstants {

    public static final String QUEUE = "seckillQueue";
    public static final String EXCHANGE = "seckillExchange";
    public static final String ROUTING_KEY = "seckill.#";

    public static final String SECKILL_RESULT_QUEUE = "seckillResultQueue";
    public static final String ORDER_RESULT_KEY = "order.result.#";

    public static final String DELETETAG_EXCHANGE = "deleteTagExchange";

    public static final String DELETETAG_QUEUE = "deleteTagQueue";

    public static final String CACHE_QUEUE = "cacheQueue";
    public static final String CACHE_EXCHANGE = "cacheExchange";
    public static final String CACHE_ROUTING_KEY = "cache.#";

}
