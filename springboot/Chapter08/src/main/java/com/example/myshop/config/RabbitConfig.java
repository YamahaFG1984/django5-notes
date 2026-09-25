package com.example.myshop.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置（≈ myshop/celery.py）。
 * Celery 自动为任务建队列；Spring AMQP 里显式声明队列 Bean，应用启动时会自动在 RabbitMQ 上创建。
 */
@Configuration
public class RabbitConfig {

    public static final String ORDER_CREATED_QUEUE = "myshop.order_created";

    @Bean
    Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE, true);   // durable：RabbitMQ 重启后队列还在
    }

    /** 消息体用 JSON（Jackson）序列化，而不是 Java 序列化；Celery 默认也用 JSON */
    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
