package com.example.myshop.orders;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.myshop.config.RabbitConfig;

/**
 * “把任务放进队列”（≈ order_created.delay(order.id)）。
 * AFTER_COMMIT：等订单真正写进数据库之后再发消息，否则消费者可能比事务更快，查不到订单。
 * Django 版没有这个问题只是因为默认自动提交；如果视图包在 atomic 里，也要用 transaction.on_commit。
 */
@Component
public class OrderTasks {

    private final RabbitTemplate rabbit;

    public OrderTasks(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        rabbit.convertAndSend(RabbitConfig.ORDER_CREATED_QUEUE, new OrderCreatedMessage(event.orderId()));
    }
}
