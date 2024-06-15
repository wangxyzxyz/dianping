package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.entity.Voucher;
import com.dianping.service.IVoucherService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description: 优惠券 Controller
 * @author Wangyw
 * @date 2024/5/25 16:53
 * @version 1.0
 */
@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;


    /**
     * @description:  新增普通优惠券
     * @param: voucher 优惠券信息
     * @author Wangyw
     * @date: 2024/6/3 0:45
     */
    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher) {
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }


    /**
     * @description:  新增秒杀类型优惠券
     * @param: voucher 优惠券信息，包含秒杀信息
     * @author Wangyw
     * @date: 2024/6/3 0:45
     */
    @PostMapping("seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }


    /**
     * @description:  查询商铺的优惠券列表
     * @param: shopId  商铺id
     * @author Wangyw
     * @date: 2024/6/3 0:46
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
       return voucherService.queryVoucherOfShop(shopId);
    }
}
