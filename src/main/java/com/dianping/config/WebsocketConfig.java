package com.dianping.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @description: WebSocket 配置
 * @author Wangyw
 * @version 1.0
 * @date 2024/6/2 0:11
 */
@Configuration
public class WebsocketConfig {

    // 注意：在单元测试时，注释掉这段代码，否则，单元测试失败
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
