package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.User;
import com.dianping.mapper.UserMapper;
import com.dianping.service.IUserService;
import com.dianping.utils.AliyunSendSms;
import com.dianping.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dianping.utils.RedisConstants.*;
import static com.dianping.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * @description:  用户登录功能 ServiceImpl
 * @author Wangyw
 * @date 2024/5/14 15:14
 * @version 1.0
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private AliyunSendSms aliyunSendSms;


    // 发送短信验证码
    @Override
    public Result sendCode(String phone) {

        // 1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 无效手机号，返回错误
            return Result.fail("手机号错误");
        }

        /* 基于 ZSet 数据结构 + 时间窗口思想，对验证码发送限制次数
         1分钟内只允许发送一次。
         10分钟内验证码获取次数不得超过三次，否则进入限制阶段，限制20分钟。
         zset数据结构记录验证码的发送时间，每次发送验证码，更新key的有效期，为10分钟。
         分别根据时间段检查1分钟内发送次数和10分钟内发送次数，10分钟内发送次数 >= 3次，存入一个String类型的key，表示进入限制，20分钟后过期。
         */

        // 2.1. 判断是否在限制条件内
        Boolean isLimit = stringRedisTemplate.hasKey(LIMIT_KEY + phone);
        if (isLimit != null && isLimit) {
            Long expire = stringRedisTemplate.getExpire(LIMIT_KEY + phone, TimeUnit.MINUTES);
            return Result.fail("您需要等待" + expire + "分钟后再发送请求");
        }
        // 2.2. 检查一分钟内发送验证码次数
        long oneMinuteAgo = System.currentTimeMillis() - 60 * 1000;
        Long count_oneMinute = stringRedisTemplate.opsForZSet().count(CODE_SENDTIME_KEY + phone, oneMinuteAgo, System.currentTimeMillis());
        if (count_oneMinute >= 1) {
            return Result.fail("距离上次发送时间不足1分钟");
        }
        // 2.3. 检查十分钟内发送验证码次数
        long tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000;
        Long count_tenMinutes = stringRedisTemplate.opsForZSet().count(CODE_SENDTIME_KEY + phone, tenMinutesAgo, System.currentTimeMillis());
        // 十分钟内超过三次
        if (count_tenMinutes >= 3) {
            // 添加一个 20 分钟内不能发验证码的限制
            stringRedisTemplate.opsForValue().set(LIMIT_KEY + phone, "1", 20, TimeUnit.MINUTES);
            // 删除记录发送时间的 zset 中的key
            stringRedisTemplate.delete(CODE_SENDTIME_KEY + phone);
            return Result.fail("请等待20分钟后再次发送");
        }

        // 3. 符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 是否调用阿里云短信服务标记
        boolean flag = false;
        if (flag) {
            // 验证码存入codeMap
            Map<String, Object> codeMap = new HashMap<>();
            codeMap.put("code", code);
            // 调用阿里云发送短信
            Boolean bool = aliyunSendSms.sendMessage(phone, codeMap);
            log.debug("生成验证码成功，为{}", code);
            if (bool) {
                stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
            }
        }else {
            // 3.1 短信发送 -> 直接打印出来
            log.debug("生成验证码成功，为{}", code);
            // 4. 保存到redis
            stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        }

        // 2.4 更新发送时间，同时刷新有效期
        stringRedisTemplate.opsForZSet().add(CODE_SENDTIME_KEY + phone, System.currentTimeMillis() + "", System.currentTimeMillis());
        stringRedisTemplate.expire(CODE_SENDTIME_KEY + phone, 10, TimeUnit.MINUTES);

        // 5. 结束
        return Result.ok();
    }


    // 登录验证功能
    @Override
    public Result login(LoginFormDTO loginForm) {
        // 1. 校验手机号
        String userPhone = loginForm.getPhone();
        // 1.1 校验格式
        if (RegexUtils.isPhoneInvalid(userPhone)) {
            // 无效手机号，返回错误
            return Result.fail("手机号错误");
        }
        // 2. 验证码
        String userCode = loginForm.getCode();
        // 2.1. 校验验证码
        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + userPhone);
        // 在进行秒杀压测的过程中，为了获取多个用户token，将验证码固定
//        String code = "000000";
        if (code == null || !code.equals(userCode)) {
            // 3. 不一致，返回错误
            return Result.fail("验证码错误");
        }

        // 4. 一致，根据手机号查询用户
        User user = query().eq("phone", userPhone).one();
        // 5. 用户不存在
        // 5.1 创建新用户
        // 5.2 保存到数据库
        if (user == null) {
            user = createUserWithPhone(userPhone);
        }
        // 6. 保存到redis
        // 6.1 随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // 6.2 将user对象转换为userDTO, 转换为hashmap结构
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString())
        );  // Long 类型的id 转化成 String
        // 6.3 存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 6.4 设置token有效期
        // 在秒杀压测过程中，不设置有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.SECONDS);

        // 7. 结束
        return Result.ok(token);
    }


    // 创建新用户，保存到数据库
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }

}
