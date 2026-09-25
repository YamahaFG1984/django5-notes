package com.example.educa.chat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 路由，≈ asgi.py 的 ProtocolTypeRouter + chat/routing.py 的 websocket_urlpatterns。
 * <p>
 * Django 需要把 WSGI 换成 ASGI（Daphne）才能处理 WebSocket；Spring Boot 的内嵌 Tomcat 同时支持 HTTP 和 WebSocket，
 * 同一个端口、同一个进程。
 */
@Configuration
@EnableWebSocket
public class ChatConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler handler;
    private final ChatHandshakeInterceptor interceptor;

    public ChatConfig(ChatWebSocketHandler handler, ChatHandshakeInterceptor interceptor) {
        this.handler = handler;
        this.interceptor = interceptor;
    }

    /**
     * 没有调用 setAllowedOrigins(...)：Spring 默认只接受<b>同源</b>的握手请求（检查 Origin 头）。
     * 书中没有用 Channels 的 AllowedHostsOriginValidator，别的网站的页面可以借用户的 cookie 连上聊天室
     * （跨站 WebSocket 劫持）。
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/chat/room/*/").addInterceptors(interceptor);
    }

    /** 订阅 chat:room:* 频道，≈ channels_redis 在后台做的事 */
    @Bean
    RedisMessageListenerContainer chatListenerContainer(RedisConnectionFactory connectionFactory, ChatRooms rooms) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(rooms, new PatternTopic(ChatRooms.CHANNEL_PREFIX + "*"));
        return container;
    }
}
