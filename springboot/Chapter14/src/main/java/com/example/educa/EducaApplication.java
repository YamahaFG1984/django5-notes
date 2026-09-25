package com.example.educa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** 项目四：在线学习平台（≈ Django 的 educa 项目）。 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class EducaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducaApplication.class, args);
    }
}
