package com.dianping.controller;

import cn.hutool.core.bean.BeanUtil;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.User;
import com.dianping.entity.UserInfo;
import com.dianping.service.IUserInfoService;
import com.dianping.service.IUserService;
import com.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description: 用户功能 Controller
 * @author Wangyw 
 * @date 2024/5/14 15:02
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;
    @Resource
    private IUserInfoService userInfoService;

    /**
     * @description: 发送短信验证码
     * @param: phone 用户手机号
     * @author Wangyw
     * @date: 2024/5/14 15:11
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone) {
        // 发送短信验证码并保存验证码
        return userService.sendCode(phone);
    }


    /** 
     * @description: 登录验证功能
     * @param: loginForm 封装的用户登录信息(phone + code)
     * @author Wangyw
     * @date: 2024/5/15 14:51
     */ 
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm){
        //实现登录功能
        return userService.login(loginForm);
    }


    /**
     * @description:  获取当前登录用户的信息
     * @author Wangyw
     * @date: 2024/6/3 0:39
     */
    @GetMapping("/me")
    public Result me(){
        //  获取当前登录的用户并返回
        UserDTO userDTO = UserHolder.getUser();
        return Result.ok(userDTO);
    }

    /**
     * @description: 获取用户详情
     * @param: userId 用户id
     * @author Wangyw
     * @date: 2024/6/3 0:43
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            return Result.ok();
        }

        // 返回
        return Result.ok(info);
    }


    /**
     * @description:  获取用户信息 (查看的其他用户的信息，从数据库得到)
     * @param: userId   其他用户的 id
     * @author Wangyw
     * @date: 2024/6/3 0:43
     */
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }
}
