package com.dianping.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.dianping.dto.OrderDetailDTO;
import com.dianping.dto.Result;
import com.dianping.service.IVoucherOrderService;
import com.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description: 秒杀订单 Controller
 * @author Wangyw
 * @date 2024/5/25 15:39
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    // 使用令牌桶算法的限流器
    private RateLimiter rateLimiter = RateLimiter.create(1000);
    @Resource
    private IVoucherOrderService voucherOrderService;


    /**
     * @description: 秒杀优惠券功能
     * @param: voucherId  优惠券id
token  用户token
     * @author Wangyw
     * @date: 2024/6/3 14:24
     */
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId, @RequestHeader("authorization") String token) {
        // 令牌桶算法限流
        if (!rateLimiter.tryAcquire(1)) {
            log.info("请求令牌失败");
            return Result.fail("网络问题，稍后重试");
        }
        return voucherOrderService.seckillVoucher(voucherId, token);
    }


    /**
     * @description: 查询订单详情
     * @param: orderNo 订单id
     * @author Wangyw
     * @date: 2024/6/3 0:47
     */
    @GetMapping("/get_order_details")
    public Result getOrderDetail(@RequestParam Long orderNo) {
        Long userId = UserHolder.getUser().getId();
        OrderDetailDTO detail = voucherOrderService.getOrderDetail(orderNo);
        // 保证用户只能查询自己的订单信息
        if (detail == null || !detail.getUser().getId().equals(userId)) {
            return Result.fail("非法操作");
        }
        return Result.ok(detail);
    }

}
