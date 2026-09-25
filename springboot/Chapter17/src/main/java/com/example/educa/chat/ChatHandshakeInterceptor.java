package com.example.educa.chat;

import java.security.Principal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.example.educa.account.CurrentUser;

/**
 * WebSocket 握手（一个普通的 HTTP GET 请求）时做检查，≈ Channels 的 AuthMiddlewareStack + URLRouter。
 * <p>
 * 书中的 ChatConsumer.connect() 什么都不检查就 accept()：任何登录用户、甚至匿名用户，
 * 都能连上任意课程的聊天室并发消息。这里要求：已登录（Spring Security 已按会话 cookie 认证过这个请求），
 * 并且选了这门课，否则返回 403，连接不会建立。
 */
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private static final Pattern ROOM = Pattern.compile("/ws/chat/room/(\\d+)/");

    private final ChatService chat;

    public ChatHandshakeInterceptor(ChatService chat) {
        this.chat = chat;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Matcher matcher = ROOM.matcher(request.getURI().getPath());
        Principal principal = request.getPrincipal();
        if (!matcher.matches()
                || !(principal instanceof Authentication auth)
                || !(auth.getPrincipal() instanceof CurrentUser user)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        long courseId = Long.parseLong(matcher.group(1));
        if (!chat.isEnrolled(courseId, user.id())) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        // ≈ self.scope['user'] 和 self.scope['url_route']['kwargs']['course_id']
        attributes.put("courseId", courseId);
        attributes.put("userId", user.id());
        attributes.put("username", user.getUsername());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
