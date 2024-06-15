package com.dianping.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @description:  拦截器，判断用户是否登录
 * @author Wangyw
 * @date 2024/5/15 15:20
 * @version 1.0
 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 判断是否需要拦截
        // 没有用户，拦截
        if (UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }
        // 2. 有用户，放行
        return true;
    }
}
