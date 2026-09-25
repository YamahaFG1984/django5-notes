package com.example.educa.chat;

import java.time.OffsetDateTime;

/**
 * 广播给房间里所有人的消息，≈ group_send 的 event 字典：
 * {'type': 'chat_message', 'message': ..., 'user': ..., 'datetime': ...}。
 * 它会被序列化成 JSON，经 Redis 发布给所有应用实例，再原样发给浏览器。
 */
public record ChatEvent(String type, long courseId, String message, String user, OffsetDateTime datetime) {

    public static ChatEvent chatMessage(long courseId, String message, String user, OffsetDateTime datetime) {
        return new ChatEvent("chat_message", courseId, message, user, datetime);
    }
}
