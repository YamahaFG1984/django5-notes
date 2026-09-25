package com.example.mysite.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自定义配置项，绑定 application.yaml 里 mysite.* 开头的键。
 * 相比 Django 在 settings.py 里随手加一个全局变量，这里是有类型、可校验、IDE 能补全的。
 *
 * @param defaultFromEmail 发件人，≈ DEFAULT_FROM_EMAIL
 */
@ConfigurationProperties(prefix = "mysite")
public record MysiteProperties(String defaultFromEmail) {
}
