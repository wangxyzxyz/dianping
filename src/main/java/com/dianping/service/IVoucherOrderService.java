package com.dianping.service;

import com.dianping.dto.OrderDetailDTO;
import com.dianping.dto.OrderMQResult;
import com.dianping.dto.OrderMessage;
import com.dianping.dto.Result;
import com.dianping.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @description:  秒杀服务 Service
 * @author Wangyw
 * @date 2024/5/25 15:41
 * @version 1.0
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId, String token);

    Long createVoucherOrder(OrderMessage orderMessage);

    OrderDetailDTO getOrderDetail(Long orderNo);

    void failedRollback(OrderMessage orderMessage);

    void changeKey(Long key);

    void checkPayTimeout(OrderMQResult orderResult);
}
