package com.example.myshop.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.myshop.config.MyshopProperties;
import com.example.myshop.config.RabbitConfig;

/**
 * 任务的执行者（≈ orders/tasks.py 的 @shared_task order_created，以及运行它的 Celery worker）。
 * Celery 需要单独启动 worker 进程；Spring AMQP 的监听器就在应用进程里运行，由它的线程池消费队列。
 * 想要独立的 worker，可以把同一个应用以“无 Web”模式再启动几份。
 */
@Component
public class OrderNotificationWorker {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationWorker.class);

    private final OrderRepository orders;
    private final MailSender mailSender;
    private final MyshopProperties properties;

    public OrderNotificationWorker(OrderRepository orders, MailSender mailSender, MyshopProperties properties) {
        this.orders = orders;
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @RabbitListener(queues = RabbitConfig.ORDER_CREATED_QUEUE)
    @Transactional(readOnly = true)
    public void orderCreated(OrderCreatedMessage message) {
        orders.findById(message.orderId()).ifPresentOrElse(order -> {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(properties.defaultFromEmail());
            mail.setTo(order.getEmail());
            mail.setSubject("Order nr. " + order.getId());
            mail.setText("Dear %s,%n%nYou have successfully placed an order. Your order ID is %d."
                    .formatted(order.getFirstName(), order.getId()));
            mailSender.send(mail);
            log.info("已发送订单 {} 的确认邮件", order.getId());
        }, () -> log.warn("订单 {} 不存在，忽略消息", message.orderId()));
    }
}
