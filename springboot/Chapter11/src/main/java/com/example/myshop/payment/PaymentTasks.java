package com.example.myshop.payment;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.myshop.config.RabbitConfig;

/** 支付状态提交到数据库之后，再把“发发票”任务放进队列。 */
@Component
public class PaymentTasks {

    private final RabbitTemplate rabbit;

    public PaymentTasks(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        rabbit.convertAndSend(RabbitConfig.PAYMENT_COMPLETED_QUEUE, new PaymentCompletedMessage(event.orderId()));
    }
}
