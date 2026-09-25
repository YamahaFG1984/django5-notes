package com.example.educa.chat;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * ≈ ChatConsumer(AsyncWebsocketConsumer)。三个回调一一对应：
 * afterConnectionEstablished ↔ connect()，handleTextMessage ↔ receive()，afterConnectionClosed ↔ disconnect()。
 * <p>
 * Channels 的消费者是 async 的，数据库操作要写 await Message.objects.acreate(...)；
 * Spring MVC 里每个 WebSocket 消息在线程池的线程上处理，直接调用普通的（阻塞的）JPA 代码即可。
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatRooms rooms;
    private final ChatService chat;
    private final ObjectMapper json;

    public ChatWebSocketHandler(ChatRooms rooms, ChatService chat, ObjectMapper json) {
        this.rooms = rooms;
        this.chat = chat;
        this.json = json;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        rooms.join(courseId(session), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        String message;
        try {
            JsonNode data = json.readTree(textMessage.getPayload());
            message = data.path("message").asString("");
        } catch (JacksonException e) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        if (message.isBlank() || message.length() > ChatService.MAX_LENGTH) {
            return;   // 忽略空消息和过长的消息
        }
        long courseId = courseId(session);
        // 书中先 group_send 再保存；这里先保存，用数据库里的时间，保证页面刷新后看到的时间与实时消息一致
        Message saved = chat.save(courseId, (Long) session.getAttributes().get("userId"), message);
        rooms.publish(ChatEvent.chatMessage(courseId, message,
                (String) session.getAttributes().get("username"), saved.getSentOn()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        rooms.leave(courseId(session), session);
    }

    private static long courseId(WebSocketSession session) {
        return (Long) session.getAttributes().get("courseId");
    }
}
