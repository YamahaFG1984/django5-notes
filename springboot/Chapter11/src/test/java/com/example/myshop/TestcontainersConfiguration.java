package com.example.myshop;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * 测试用的 RabbitMQ 和 Redis。
 * 容器放在静态字段里：即使因为配置不同产生了多个 Spring 上下文，也共用同一组容器，不会重复启动。
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management");
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.2").withExposedPorts(6379);

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbit() {
        return RABBIT;
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redis() {
        return REDIS;
    }
}
