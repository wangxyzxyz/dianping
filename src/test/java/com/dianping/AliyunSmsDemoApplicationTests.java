package com.dianping;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

/**
 * @description: 测试阿里云短信服务发送功能
 * @author Wangyw
 * @date 2024/5/25 17:32
 * @version 1.0
 */
@SpringBootTest
public class AliyunSmsDemoApplicationTests {

    @Test
    void contextLoads() {

        /**
         * 连接阿里云：
         *
         * 三个参数：
         * regionId 不要动，默认使用官方的
         * accessKeyId 自己的用户accessKeyId
         * accessSecret 自己的用户accessSecret
         */
        DefaultProfile profile = DefaultProfile.getProfile(
                "cn-hangzhou", "xxx", "xxx");
        IAcsClient client = new DefaultAcsClient(profile);

        // 构建请求：
        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");

        // 自定义参数：
        request.putQueryParameter("PhoneNumbers", "xxx");// 接收短信的手机号
        request.putQueryParameter("SignName", "xxx");// 短信签名
        request.putQueryParameter("TemplateCode", "xxx");// 短信模版CODE

        // 构建短信验证码
        Map<String,Object> map = new HashMap<>();
        map.put("code",1234);// 这里仅用于测试，所以验证码写死
        request.putQueryParameter("TemplateParam", JSONObject.toJSONString(map));

        try {
            CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }
}
