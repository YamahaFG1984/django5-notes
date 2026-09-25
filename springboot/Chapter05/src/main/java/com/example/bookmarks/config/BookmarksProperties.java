package com.example.bookmarks.config;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * bookmarks.* 自定义配置。
 *
 * @param defaultFromEmail     发件人，≈ DEFAULT_FROM_EMAIL
 * @param secretKey            用于给密码重置链接签名，≈ SECRET_KEY（生产环境必须通过环境变量覆盖）
 * @param mediaRoot            用户上传文件的保存目录，≈ MEDIA_ROOT
 * @param passwordResetTimeout 重置链接有效期，≈ PASSWORD_RESET_TIMEOUT（默认 3 天）
 */
@ConfigurationProperties(prefix = "bookmarks")
public record BookmarksProperties(
        @DefaultValue("Bookmarks <noreply@example.com>") String defaultFromEmail,
        String secretKey,
        @DefaultValue("media") Path mediaRoot,
        @DefaultValue("3d") Duration passwordResetTimeout) {
}
