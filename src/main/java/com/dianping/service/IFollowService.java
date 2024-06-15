package com.dianping.service;

import com.dianping.dto.Result;
import com.dianping.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @description: 用户关注 Service
 * @author Wangyw
 * @date 2024/5/25 16:48
 * @version 1.0
 */
public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);
}
