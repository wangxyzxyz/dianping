package com.dianping.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @description: 封装创建订单信息, 用于异步创建订单
 * @author Wangyw
 * @date 2024/6/3 0:51
 * @version 1.0
 */
@Data
public class OrderMessage implements Serializable {

    private Long voucherId;
    private Long userId;
    private String token;
}
