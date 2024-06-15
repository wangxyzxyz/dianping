package com.dianping.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.dianping.utils.MQConstants.*;

/**
 * @description:  RabbitMQ 消息队列配置
 * @author Wangyw
 * @date 2024/5/25 20:25
 * @version 1.0
 */
@Configuration
public class RabbitMQConfig {

    // 秒杀任务queue
    @Bean
    public Queue queue() {
        return new Queue(QUEUE);
    }

    // 秒杀任务exchange
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE);
    }

    // 秒杀绑定  key：seckill.#
    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue()).to(topicExchange()).with(ROUTING_KEY);
    }

    // 秒杀结果queue
    @Bean
    public Queue orderResultQueue() {
        return new Queue(SECKILL_RESULT_QUEUE);
    }

    // 秒杀结果绑定 key: order.result.#
    @Bean
    public Binding orderResultBinding() {
        return BindingBuilder.bind(orderResultQueue()).to(topicExchange()).with(ORDER_RESULT_KEY);
    }

    // 修改内存标识exchange
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(DELETETAG_EXCHANGE);
    }

    // 修改内存标识queue
    @Bean
    public Queue deleteQueue() {
        return new Queue(DELETETAG_QUEUE);
    }

    // 修改内存标识绑定
    @Bean
    public Binding deleteBinding() {
        return BindingBuilder.bind(deleteQueue()).to(fanoutExchange());
    }

    // 缓存数据删除queue
    @Bean
    public Queue cacheQueue() {
        return new Queue(CACHE_QUEUE);
    }

    // 缓存数据删除exchange
    @Bean
    public TopicExchange cacheExchange() {
        return new TopicExchange(CACHE_EXCHANGE);
    }

    // 缓存数据删除绑定
    @Bean
    public Binding cacheBinding() {
        return BindingBuilder.bind(cacheQueue()).to(cacheExchange()).with(CACHE_ROUTING_KEY);
    }
}
