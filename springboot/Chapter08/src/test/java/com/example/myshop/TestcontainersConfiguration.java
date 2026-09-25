package com.example.myshop;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * 测试用的 RabbitMQ；@ServiceConnection 自动填写 spring.rabbitmq.*。
 * 容器放在静态字段里：即使产生了多个 Spring 上下文，也共用同一个容器。
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management");

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbit() {
        return RABBIT;
    }
}
