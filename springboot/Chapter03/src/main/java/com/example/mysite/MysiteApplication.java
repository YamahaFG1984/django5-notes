package com.example.mysite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 入口类。@SpringBootApplication 会扫描本包及子包里的组件，
 * 大致相当于 Django 的 settings.INSTALLED_APPS + manage.py runserver。
 * @ConfigurationPropertiesScan 让 @ConfigurationProperties 类（如 MysiteProperties）生效。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MysiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(MysiteApplication.class, args);
    }
}
