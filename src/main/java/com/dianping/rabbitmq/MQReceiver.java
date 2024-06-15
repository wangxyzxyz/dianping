package com.dianping.rabbitmq;

import com.alibaba.fastjson.JSON;
import com.dianping.dto.OrderMQResult;
import com.dianping.dto.OrderMessage;
import com.dianping.service.ISeckillVoucherService;
import com.dianping.service.IVoucherOrderService;
import com.dianping.utils.MQConstants;
import com.dianping.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.websocket.Session;

/**
 * @description: RabbitMQ 消息接收处理单元
 * @author Wangyw
 * @date 2024/5/18 15:22
 * @version 1.0
 */
@Slf4j
@Service
public class MQReceiver {
    @Resource
    private IVoucherOrderService voucherOrderService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;


    // 接收创建订单消息，处理秒杀订单
    @RabbitListener(queues = MQConstants.QUEUE)
    public void receiveSeckillMessage(String msg) {
        log.info("接收到创建订单消息" + msg);

        OrderMessage orderMessage = JSON.parseObject(msg, OrderMessage.class);

        // 订单结果
        OrderMQResult result = new OrderMQResult();
        result.setToken(orderMessage.getToken());
        result.setSeckillId(orderMessage.getVoucherId());
        result.setUserId(orderMessage.getUserId());

        try {
            Long orderNo = voucherOrderService.createVoucherOrder(orderMessage);
            // 订单创建成功
            result.setCode(200);
            result.setMsg("订单创建成功");
            result.setOrderNo(orderNo);

            // 当下单成功后，发送延迟消息，检查订单支付状态，超过时间未支付，取消该订单
            // 打印发送消息时间戳
            long l = System.currentTimeMillis();
            log.info(String.valueOf(l));
            // 2.发送消息，利用消息后置处理器添加消息头
            rabbitTemplate.convertAndSend("delay.direct", "delay", JSON.toJSONString(result), (MessagePostProcessor) message -> {
                // 添加延迟消息属性
                // 实验场景，设置延迟时间1分钟
                message.getMessageProperties().setDelay(1000 * 60);
                return message;
            });
        } catch (Exception e) {
            // 订单创建失败
            result.setCode(500);
            result.setMsg("订单创建失败");
            // 下单失败回滚redis中数据，同时修改本地下单标识
            voucherOrderService.failedRollback(orderMessage);
        }
        // 发送订单创建结果消息
        rabbitTemplate.convertAndSend(MQConstants.EXCHANGE, "order.result.message", JSON.toJSONString(result));

    }


    // 接收修改本地标识消息，更改本地标识
    @RabbitListener(queues = MQConstants.DELETETAG_QUEUE)
    public void deleteTagMessage(String msg) {
        log.info("[修改本地标识] 收到修改本地标识消息：{}", msg);
        Long seckillId = Long.parseLong(msg);
        voucherOrderService.changeKey(seckillId);
    }


    // 监听延迟消息，收到消息，执行订单超时未支付检查
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "delay.queue", durable = "true"),
            exchange = @Exchange(name = "delay.direct", delayed = "true"),
            key = "delay"
    ))
    public void listenDelayMessage(String msg){
        log.info("接收到delay.queue的延迟消息：{}", msg);
        long l = System.currentTimeMillis();
        log.info(String.valueOf(l));
        OrderMQResult orderResult = JSON.parseObject(msg, OrderMQResult.class);
        voucherOrderService.checkPayTimeout(orderResult);

    }


    // 监听删除缓存消息，利用mq重试机制，保证缓存删除成功
    @RabbitListener(queues = MQConstants.CACHE_QUEUE)
    public void receiveCacheDelMessage(String key) {
        // 利用重试机制
        log.info("删除缓存key...");
        stringRedisTemplate.delete(key);
        log.info("删除成功，消息处理完成");
    }


    // 监听订单创建结果，收到消息通过 websocket 发送到前端
    @RabbitListener(queues = MQConstants.SECKILL_RESULT_QUEUE)
    public void seckillResultMessage(String msg) {
        log.info("[订单结果] 收到创建订单结果消息：{}", msg);
        OrderMQResult result = JSON.parseObject(msg, OrderMQResult.class);

        try {
            int count = 0;
            // 循环：有可能前端建立socket连接 慢于 消息队列处理订单任务，多循环几次保证不丢失
            do {
                // 根据token获取session对象
                Session session = WebSocketServer.SESSION_MAP.get(result.getToken());
                if (session != null) {
                    // 通过session，将数据写入前端
                    session.getBasicRemote().sendText(msg);
                    return;
                }
                count++;
                // 睡一会
                Thread.sleep(300);
            }while (count <= 10);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
