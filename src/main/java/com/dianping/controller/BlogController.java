package com.dianping.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.Blog;
import com.dianping.service.IBlogService;
import com.dianping.utils.SystemConstants;
import com.dianping.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: 商铺笔记 Controller
 * @author Wangyw
 * @date 2024/5/25 16:45
 * @version 1.0
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    /**
     * @description:  保存笔记信息到数据库
     * @param: blog   笔记
     * @author Wangyw
     * @date: 2024/6/4 14:28
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    /**
     * @description:  根据笔记id查询详细信息
     * @param: id       blog-id
     * @author Wangyw
     * @date: 2024/6/4 14:29
     */
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }

    /**
     * @description:  对笔记点赞功能的实现
     * @param: id   blog-id
     * @author Wangyw
     * @date: 2024/6/4 14:32
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }


    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询是否点赞
        records.forEach((blog -> {
            blogService.isBlogLiked(blog);
        }));
        return Result.ok(records);
    }


    /**
     * @description:  查询热门笔记，按点赞数量排序
     * @param: current  页数
     * @author Wangyw
     * @date: 2024/6/6 15:32
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    /**
     * @description: 查询该笔记的前N个点赞用户
     * @param: id  笔记id
     * @author Wangyw
     * @date: 2024/6/4 14:53
     */
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id){
        return blogService.queryBloglikes(id);
    }


    /**
     * @description:  根据用户的id查询其所有的笔记
     * @param: current  页码
    id  其他用户id
     * @author Wangyw
     * @date: 2024/6/5 14:59
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(@RequestParam(value = "current", defaultValue = "1") Integer current,
                                  @RequestParam("id") Long id) {
        Page<Blog> page = blogService.query()
                .eq("user_id", id)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }


    /**
     * @description:  分页查询关注用户的笔记,采用滚动查询的方式，防止查到重复的数据
     * @param: max  上次查询最小时间戳
    offset  偏移量，是考虑到score相同，防止查到重复数据，设置每次查询的偏移量
     * @author Wangyw
     * @date: 2024/6/6 14:50
     */
    @GetMapping("of/follow")
    public Result queryBlogOfFollow(
            @RequestParam("lastId") Long max,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return blogService.queryBlogOfFollow(max, offset);
    }
}
