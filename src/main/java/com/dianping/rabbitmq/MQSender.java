package com.dianping.rabbitmq;

import com.dianping.utils.MQConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @description: RabbitMQ 消息发送单元
 * @author Wangyw
 * @date 2024/5/18 15:16
 * @version 1.0
 */
@Slf4j
@Service
public class MQSender {
    @Resource
    private RabbitTemplate rabbitTemplate;

    // 秒杀处理的 routing_key
    private static final String ROUTING_KEY = "seckill.message";
    // 缓存删除的 routing_key
    private static final String CACHE_ROUTING_KEY = "cache.message";


    // 发送秒杀消息 (orderMessage)，异步处理
    public void sendSeckillMessage(String msg) {
        log.info("发送创建订单消息" + msg);
        rabbitTemplate.convertAndSend(MQConstants.EXCHANGE, ROUTING_KEY, msg);
    }


    // 发送删除缓存的消息
    public void sendCacheDelMessage(String key) {
        log.info("发送消息" + key);
        rabbitTemplate.convertAndSend(MQConstants.CACHE_EXCHANGE, CACHE_ROUTING_KEY, key);

    }

    // 发送删除本地标识消息
    public void sendRollbackMsg(String msg) {
        log.info("发送修改本地标识消息：{}", msg);
        rabbitTemplate.convertAndSend(MQConstants.DELETETAG_EXCHANGE, "", msg);
    }
}
