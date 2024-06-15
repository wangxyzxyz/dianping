package com.dianping.dto;

import com.dianping.entity.User;
import com.dianping.entity.Voucher;
import com.dianping.entity.VoucherOrder;
import lombok.Data;

/**
 * @description: 封装订单详情信息
 * @author Wangyw
 * @date 2024/6/3 0:50
 * @version 1.0
 */
@Data
public class OrderDetailDTO {
    private VoucherOrder orders;
    private Voucher voucher;
    private User user;
}
