package com.dianping.service;

import com.dianping.dto.Result;
import com.dianping.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @description: 优惠券功能 Service
 * @author Wangyw
 * @date 2024/5/25 16:53
 * @version 1.0
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
