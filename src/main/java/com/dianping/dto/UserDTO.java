package com.dianping.dto;

import lombok.Data;

/**
 * @description: 封装脱敏用户信息
 * @author Wangyw
 * @date 2024/6/3 0:53
 * @version 1.0
 */
@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
}
