package com.dianping.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description:  Websocket 服务
 * @author Wangyw
 * @version 1.0
 * @date 2024/6/2 0:17
 */
@ServerEndpoint("/ws/{token}")
@Component
@Slf4j
public class WebSocketServer {

    // 存储 token-session
    public static Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session,@PathParam("token") String token) {
        log.info("[websocket] 有新的客户端连接: {}", token);
        SESSION_MAP.put(token, session);
    }

    @OnClose
    public void onClose(@PathParam("token") String token) {
        log.info("[websocket] 连接准备关闭: {}", token);
        SESSION_MAP.remove(token);
    }

    @OnError
    public void onError(Throwable throwable) {
        log.warn("[websocket] 连接出现异常", throwable);
    }

}
