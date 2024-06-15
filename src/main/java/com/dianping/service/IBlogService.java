package com.dianping.service;

import com.dianping.dto.Result;
import com.dianping.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @description:  商铺笔记 Service
 * @author Wangyw
 * @date 2024/5/25 16:46
 * @version 1.0
 */
public interface IBlogService extends IService<Blog> {

    Result saveBlog(Blog blog);

    Result queryBlogById(Long id);

    void isBlogLiked(Blog blog);

    Result likeBlog(Long id);

    Result queryHotBlog(Integer current);

    Result queryBloglikes(Long id);

    Result queryBlogOfFollow(Long max, Integer offset);
}
