package com.example.myshop.orders;

/** 发往 RabbitMQ 的消息体，会被序列化成 JSON：{"orderId": 1}。≈ order_created.delay(order.id) 的参数 */
public record OrderCreatedMessage(Long orderId) {
}
