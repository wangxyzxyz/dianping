package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.entity.User;

/**
 * @description:  用户登录功能 Service
 * @author Wangyw
 * @date 2024/5/14 15:13
 * @version 1.0
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone);

    Result login(LoginFormDTO loginForm);
}
