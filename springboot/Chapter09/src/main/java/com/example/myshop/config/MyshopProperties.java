package com.example.myshop.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param defaultFromEmail 订单通知邮件的发件人（书中写死为 admin@myshop.com）
 * @param mediaRoot        商品图片等上传文件的目录，≈ MEDIA_ROOT
 */
@ConfigurationProperties(prefix = "myshop")
public record MyshopProperties(
        @DefaultValue("admin@myshop.com") String defaultFromEmail,
        @DefaultValue("media") Path mediaRoot) {
}
