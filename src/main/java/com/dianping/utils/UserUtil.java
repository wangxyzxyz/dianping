package com.dianping.utils;

import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dianping.dto.Result;
import com.dianping.entity.User;
import com.dianping.service.IUserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.dianping.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * @description: 生成用户工具类，生成用于压测的用户数据
 * @author Wangyw
 * @date 2024/5/25 20:04
 * @version 1.0
 */
@Component
public class UserUtil {
    @Resource
    private IUserService userService;


    // 生成用户数据，获取token
    public void createUser(int count) throws Exception {
        List<User> users = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            User user = new User();
            Long phone = 13000000000L + i;
            user.setPhone(phone.toString());
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
            users.add(user);
        }
        // 保存到数据库
        userService.saveBatch(users);
        System.out.println("插入数据库");
        // 登录，生成token
        String urlString = "http://localhost:8081/user/login";
        File file = new File("C:\\Users\\Lenovo\\Desktop\\config.txt");
        if (file.exists()) {
            file.delete();
        }
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.seek(0);
        for (int i = 0; i < count; i++) {
            User user = users.get(i);
            URL url = new URL(urlString);
            HttpURLConnection co = (HttpURLConnection) url.openConnection();
            co.setRequestMethod("POST");
            co.setDoOutput(true);
            co.setRequestProperty("accept", "*/*");
            co.setRequestProperty("connection", "Keep-Alive");
            co.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            co.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            OutputStream out = co.getOutputStream();
            String params = "{\"phone\":" + "\"" + user.getPhone() + "\"" + ",\"code\":\"000000\"}";
            out.write(params.getBytes());
            out.flush();
            InputStream inputStream = co.getInputStream();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buff = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(buff)) >= 0) {
                bout.write(buff, 0, len);
            }
            inputStream.close();
            bout.close();
            String response = new String(bout.toByteArray());
            ObjectMapper mapper = new ObjectMapper();
            Result result = mapper.readValue(response, Result.class);
            String token = (String) result.getData();
            System.out.println("token:" + token);
            raf.seek(raf.length());
            raf.write(token.getBytes());
            raf.write("\r\n".getBytes());
            System.out.println("write to file: " + user.getId());

        }
        raf.close();
        System.out.println("over");
    }

}
