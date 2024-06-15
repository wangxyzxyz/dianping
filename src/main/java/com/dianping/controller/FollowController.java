package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @description: 用户关注 Controller
 * @author Wangyw
 * @date 2024/5/25 16:47
 * @version 1.0
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    /** 
     * @description:  关注和取关功能
     * @param: followUserId  发布笔记的其他用户的id
isFollow    true: 关注    false: 取关
     * @author Wangyw
     * @date: 2024/6/4 15:10
     */ 
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId,
                         @PathVariable("isFollow") Boolean isFollow) {
        return followService.follow(followUserId, isFollow);
    }


    /** 
     * @description:  判断是否关注
     * @param: followUserId   发布笔记的其他用户的id
     * @author Wangyw
     * @date: 2024/6/4 15:07
     */ 
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        return followService.isFollow(followUserId);
    }


    /**
     * @description:  获取当前用户与博主的共同关注
     * @param: id     发布笔记的其他用户的id
     * @author Wangyw
     * @date: 2024/6/5 15:08
     */
    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long id) {
        return followService.followCommons(id);
    }

}
