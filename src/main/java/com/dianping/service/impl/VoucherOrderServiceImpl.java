package com.dianping.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.dianping.dto.OrderDetailDTO;
import com.dianping.dto.OrderMQResult;
import com.dianping.dto.OrderMessage;
import com.dianping.dto.Result;
import com.dianping.entity.SeckillVoucher;
import com.dianping.entity.User;
import com.dianping.entity.Voucher;
import com.dianping.entity.VoucherOrder;
import com.dianping.mapper.VoucherOrderMapper;
import com.dianping.rabbitmq.MQSender;
import com.dianping.service.ISeckillVoucherService;
import com.dianping.service.IUserService;
import com.dianping.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.service.IVoucherService;
import com.dianping.utils.UserHolder;
import com.sankuai.inf.leaf.service.SegmentService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dianping.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * @description: 秒杀服务 ServiceImpl
 * @author Wangyw
 * @date 2024/5/25 15:42
 * @version 1.0
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService, InitializingBean {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private IVoucherService voucherService;
    @Resource
    private IUserService userService;

    // 基于 redis 的全局唯一id生成器
//    @Resource
//    private RedisIdWorker redisIdWorker;

    // 美团 leaf（基于数据库）id生成器
    @Resource
    private SegmentService segmentService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MQSender mqSender;

    // 库存为空的内存标记, 线程安全的map
    private static Map<Long, Boolean> EmptyStockMap = new ConcurrentHashMap<>();

    // 加载lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static{
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    // 秒杀优惠券功能实现
    @Override
    public Result seckillVoucher(Long voucherId, String token) {
        // 库存内存标识判断
        if (EmptyStockMap.get(voucherId)) {
            return Result.fail("库存不足。state:0");
        }
        // 1. 获取用户id
        Long userId = UserHolder.getUser().getId();
        // 2. 执行lua脚本，判断结果
        // 库存预减，判断是否一人一单
        // 可进一步优化：lua脚本放入redis服务器执行，减少网络传输损耗
        Long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );
        int result = res.intValue();
        // 不为0，没有购买资格
        if (result != 0) {
            if (result == 1) {
                // 库存预减未成功
                EmptyStockMap.put(voucherId, true);
            }
            return Result.fail(result == 1 ? "库存不足" : "不能重复下单");
        }

        // 可以购买
        // 创建消息
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setVoucherId(voucherId);
        orderMessage.setUserId(userId);
        orderMessage.setToken(token);
        // 这块将优惠券id + 用户id + token 发送出去
        // 发送到MQ
        mqSender.sendSeckillMessage(JSON.toJSONString(orderMessage));
        // 返回结果
        return Result.ok("创建订单中，请稍后...");
    }


    // 创建订单，保存订单到数据库
    // 添加事务
    @Transactional
    @Override
    public Long createVoucherOrder(OrderMessage orderMessage) {

        // 根据优惠券id和用户id查询订单
        Long userId = orderMessage.getUserId();
        Long voucherId = orderMessage.getVoucherId();
        // 一人一单
        int count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
        if (count >= 1) {
            throw new RuntimeException("重复下单");
        }
        // 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            throw new RuntimeException("库存不足");
        }
        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // .1 订单id
        // 基于 redis 生成唯一 id
//        long orderId = redisIdWorker.nextId("order");
        // 使用美团 leaf 生成id
        long orderId = segmentService.getId("seckill").getId();
        voucherOrder.setId(orderId);
        // .2 用户id
        voucherOrder.setUserId(userId);
        // .3 代金券id
        voucherOrder.setVoucherId(voucherId);

        // 保存订单
        boolean isSuccess = save(voucherOrder);
        if (!isSuccess) {
            throw new RuntimeException("保存订单信息失败");
        }

        // 返回订单id
        return orderId;
    }


    // 查询订单详情，返回具体订单，优惠券，用户信息 -> OrderDetailDTO
    @Override
    public OrderDetailDTO getOrderDetail(Long orderNo) {
        if (orderNo == null) {
            return null;
        }
        OrderDetailDTO detail = new OrderDetailDTO();
        VoucherOrder order = getById(orderNo);
        if (order == null) {
            return null;
        }
        Voucher voucher = voucherService.getById(order.getVoucherId());
        User user = userService.getById(order.getUserId());
        detail.setOrders(order);
        detail.setVoucher(voucher);
        detail.setUser(user);

        return detail;
    }


    // 创建订单失败回滚 redis
    @Override
    public void failedRollback(OrderMessage orderMessage) {
        String voucherKey = SECKILL_STOCK_KEY + orderMessage.getVoucherId();
        String orderKey = "seckill:order:" + orderMessage.getVoucherId();
        // 1. redis库存+1
        stringRedisTemplate.opsForValue().increment(voucherKey);
        // 2. 删除redis用户下单标记
        stringRedisTemplate.opsForSet().remove(orderKey, orderMessage.getUserId().toString());
        // 3. 删除本地标识，考虑服务器集群下采用消息队列，通知到其他服务器
        mqSender.sendRollbackMsg(orderMessage.getVoucherId().toString());
    }


    // 修改本地标识
    @Override
    public void changeKey(Long key) {
        EmptyStockMap.put(key, false);
    }


    // 订单超时未支付检查
    @Transactional
    @Override
    public void checkPayTimeout(OrderMQResult orderResult) {
        // 基于订单编号查询订单
        VoucherOrder order = getById(orderResult.getOrderNo());
        // 判断状态是否为未支付（1），说明需要取消
        if (order.getStatus() != 1) {
            // 否则，不管
            return;
        }
        // mysql库存+1，订单删除
        seckillVoucherService.update()
                .setSql("stock = stock + 1")
                .eq("voucher_id", orderResult.getSeckillId())
                .update();
        removeById(orderResult.getOrderNo());
        // redis回滚
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setVoucherId(orderResult.getSeckillId());
        orderMessage.setUserId(orderResult.getUserId());
        failedRollback(orderMessage);
    }


    // 在 Bean 初始化时执行的方法
    @Override
    public void afterPropertiesSet() {
        // 向 redis 同步库存信息，向本地标识写入数据
        List<SeckillVoucher> list = seckillVoucherService.list();
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        list.forEach(voucher ->{
            stringRedisTemplate.opsForValue().set("seckill:stock:" + voucher.getVoucherId(), voucher.getStock().toString());
            EmptyStockMap.put(voucher.getVoucherId(), false);
        });
    }


    /*
    // 单机情况下，解决库存超卖，保证一人一单
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1. 查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        // 2. 判断秒杀是否开始
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀未开始");
        }
        // 3. 判断秒杀是否结束
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }
        // 4. 判断库存是否充足
        if (seckillVoucher.getStock() <= 0) {
            return Result.fail("库存不足");
        }

        // 在这里加锁
        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()) {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    // 封装成方法
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 一人一单
        // 根据优惠券id和用户id查询订单
        Long userId = UserHolder.getUser().getId();
        int count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
        if (count >= 1) {
            return Result.fail("用户已经秒杀过了");
        }

        // 5. 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }
        // 6. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6.1 订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 6.2 用户id
        voucherOrder.setUserId(userId);
        // 6.3 代金券id
        voucherOrder.setVoucherId(voucherId);

        // 保存订单
        save(voucherOrder);

        // 7. 返回订单id
        return Result.ok(orderId);
    }

     */

}


