package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.dto.Result;
import com.dianping.dto.ScrollResult;
import com.dianping.dto.UserDTO;
import com.dianping.entity.Blog;
import com.dianping.entity.Follow;
import com.dianping.entity.User;
import com.dianping.mapper.BlogMapper;
import com.dianping.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.service.IFollowService;
import com.dianping.service.IUserService;
import com.dianping.utils.SystemConstants;
import com.dianping.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dianping.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.dianping.utils.RedisConstants.FEED_KEY;

/**
 * @description: 商铺笔记 ServiceImpl
 * @author Wangyw
 * @date 2024/5/25 16:46
 * @version 1.0
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;


    // 保存笔记,同时推送到粉丝的收件箱 (redis)
    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSuccess = save(blog);
        if (!isSuccess) {
            return Result.fail("新增笔记失败");
        }
        // 查询笔记作者的所有粉丝
        List<Follow> follows = followService.query()
                .eq("follow_user_id", user.getId()).list();
        // 推送笔记id给所有粉丝
        for (Follow follow : follows) {
            // 获取粉丝的id
            Long userId = follow.getUserId();
            // 进行推送
            String key = FEED_KEY + userId;
            stringRedisTemplate.opsForZSet()
                    .add(key, blog.getId().toString(), System.currentTimeMillis());
        }
        // 返回id
        return Result.ok(blog.getId());
    }


    // 查询热门笔记
    @Override
    public Result queryHotBlog(Integer current) {
        // 分页查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 添加用户和是否点赞信息
        records.forEach(blog ->{
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }


    // 查询该笔记的前TopN的点赞用户
    @Override
    public Result queryBloglikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        // 1. 在redis查询前top5的点赞用户
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 2. 解析用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        // 3. 根据用户id查询用户
        String idStr = StrUtil.join(",", ids);
        List<UserDTO> userDTOs = userService.query()
                .in("id", ids).last("order by field(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 4. 返回
        return Result.ok(userDTOs);
    }


    // 查询已关注博主的笔记，实现 分页查询 收件箱内容
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2. 查询收件箱
        String key = FEED_KEY + userId;
        // 从大到小，offset：从查询的结果开始的偏移量，count：查几条
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        // 3. 非空判断
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        // 4. 解析数据：blogId， minTime， offset
        ArrayList<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            // 4.1 获取id
            ids.add(Long.valueOf(tuple.getValue()));
            // 4.2 获取分数
            long time = tuple.getScore().longValue();
            if (time == minTime) {
                os++;
            }else {
                minTime = time;
                os = 1;
            }
        }
        // 有重复时间戳 时考虑的
        os = minTime == max ? os + offset : os;
        // 5. 根据id查询blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Blog blog : blogs) {
            // 5.1 查询blog有关的用户
            queryBlogUser(blog);
            // 5.2 查询blog是否被点赞
            isBlogLiked(blog);
        }
        // 6. 封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);

        return Result.ok(r);
    }


    // 查看笔记
    @Override
    public Result queryBlogById(Long id) {
        // 1. 查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在");
        }
        // 2. 查询blog有关用户
        queryBlogUser(blog);
        // 3. 查询blog是否被点赞
        isBlogLiked(blog);

        return Result.ok(blog);

    }


    // 查询笔记是否被当前用户点赞
    public void isBlogLiked(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录， 不进行操作
            return;
        }
        // 判断用户是否点赞
        Long userId = user.getId();
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }


    // 进行点赞，同时判断是否点赞过，点赞过取消点赞
    @Override
    public Result likeBlog(Long id) {
        // 1. 获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 2. 判读当前用户是否点赞
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 3. 未点赞，进行点赞
            // 3.1 数据库点赞数 +1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            if (isSuccess) {
                // 3.2 保存用户到redis的 set 集合
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        }else {
            // 4. 如果已经点赞，取消点赞
            // 4.1 数据库点赞数 -1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if (isSuccess) {
                // 4.2 把用户从set集合移除
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }

        return Result.ok();
    }


    // 查询用户信息，保存到笔记中
    private void queryBlogUser(Blog blog) {
        // 1. 得到用户id
        Long userId = blog.getUserId();
        // 2. 根据用户id查用户
        User user = userService.getById(userId);
        // 3. 放入blog中
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

}
