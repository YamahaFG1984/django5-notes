package com.example.mysite;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 测试时用 Docker 启动一个一次性的 PostgreSQL 16。
 * @ServiceConnection 会自动把容器的地址、用户名、密码填进 spring.datasource.*，不用写任何配置。
 * （Django 的测试运行器会自动创建 test_ 前缀的测试库，思路相同。）
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:16");
    }
}
