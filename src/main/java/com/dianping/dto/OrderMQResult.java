package com.dianping.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @description: 封装 订单创建结果 信息
 * @author Wangyw
 * @date 2024/6/3 0:51
 * @version 1.0
 */
@Data
public class OrderMQResult implements Serializable {
    private Long seckillId;
    private Long userId;
    private Long orderNo;
    private String token;
    private String msg = "订单创建成功";  //提示消息
    private Integer code = 200; //状态码
}
