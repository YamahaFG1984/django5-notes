package com.example.educa;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;

/** 测试用的 Redis（缓存后端）。静态字段：所有测试上下文共用一个容器。 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.2").withExposedPorts(6379);

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redis() {
        return REDIS;
    }
}
