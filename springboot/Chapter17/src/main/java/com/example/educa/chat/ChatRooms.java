package com.example.educa.chat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import tools.jackson.databind.ObjectMapper;

/**
 * 聊天室的“通道层”，≈ Channels 的 RedisChannelLayer（group_add / group_discard / group_send）。
 * <ul>
 *   <li>本机：记录每个房间里有哪些 WebSocket 连接；</li>
 *   <li>跨实例：消息发布到 Redis 频道 chat:room:&lt;课程 id&gt;，每个应用实例都订阅了 chat:room:*，
 *       收到后发给本机该房间里的连接。只部署一个实例时 Redis 看似多余，但扩容时代码不用改。</li>
 * </ul>
 */
@Component
public class ChatRooms implements MessageListener {

    public static final String CHANNEL_PREFIX = "chat:room:";

    private static final Logger log = LoggerFactory.getLogger(ChatRooms.class);

    /** 课程 id → (会话 id → 会话) */
    private final Map<Long, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    public ChatRooms(StringRedisTemplate redis, ObjectMapper json) {
        this.redis = redis;
        this.json = json;
    }

    /**
     * group_add。WebSocketSession.sendMessage 不是线程安全的（两个线程同时向一个连接发消息会出错），
     * 所以包一层 ConcurrentWebSocketSessionDecorator：串行发送，并限制发送超时和缓冲区大小，防止慢客户端拖垮服务器。
     */
    public void join(long courseId, WebSocketSession session) {
        rooms.computeIfAbsent(courseId, id -> new ConcurrentHashMap<>())
                .put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, 5_000, 64 * 1024));
    }

    /** group_discard */
    public void leave(long courseId, WebSocketSession session) {
        rooms.computeIfPresent(courseId, (id, sessions) -> {
            sessions.remove(session.getId());
            return sessions.isEmpty() ? null : sessions;
        });
    }

    /** group_send：发布到 Redis，而不是直接发给本机的连接 */
    public void publish(ChatEvent event) {
        redis.convertAndSend(CHANNEL_PREFIX + event.courseId(), json.writeValueAsString(event));
    }

    /** Redis 订阅回调：≈ consumer 的 chat_message(self, event) —— 把事件原样发给浏览器 */
    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        long courseId = Long.parseLong(channel.substring(CHANNEL_PREFIX.length()));
        TextMessage text = new TextMessage(new String(message.getBody(), StandardCharsets.UTF_8));
        rooms.getOrDefault(courseId, Map.of()).values().forEach(session -> {
            try {
                session.sendMessage(text);
            } catch (Exception e) {
                log.debug("Could not deliver chat message to {}", session.getId(), e);
            }
        });
    }

    /** 测试和监控用：某个房间当前的连接数 */
    public int size(long courseId) {
        return rooms.getOrDefault(courseId, Map.of()).size();
    }
}
