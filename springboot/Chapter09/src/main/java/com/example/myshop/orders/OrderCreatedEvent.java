package com.example.myshop.orders;

/** 应用内事件：订单已创建（在事务中发布，事务提交后才会被 OrderTasks 处理）。 */
public record OrderCreatedEvent(Long orderId) {
}
