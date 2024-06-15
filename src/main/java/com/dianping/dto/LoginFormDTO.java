package com.dianping.dto;

import lombok.Data;

/**
 * @description: 封装登录信息
 * @author Wangyw
 * @date 2024/6/3 0:50
 * @version 1.0
 */
@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
