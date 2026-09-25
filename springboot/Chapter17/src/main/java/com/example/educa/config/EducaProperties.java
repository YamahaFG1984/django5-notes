package com.example.educa.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 项目自己的配置项（application.yaml 里 educa.* 开头的部分）。
 *
 * @param mediaRoot        上传文件目录，≈ MEDIA_ROOT
 * @param siteDomain       网站的主域名，例如 educaproject.com；设置后 &lt;课程 slug&gt;.educaproject.com 会跳转到课程页面。
 *                         为空时不启用子域名跳转（本地开发）
 * @param defaultFromEmail 发件人，≈ DEFAULT_FROM_EMAIL
 * @param requireHttps     把 HTTP 请求重定向到 HTTPS，≈ SECURE_SSL_REDIRECT
 * @param allowedHosts     允许的 Host 头，≈ ALLOWED_HOSTS；以点开头表示“该域名及其所有子域名”。为空时不检查
 */
@ConfigurationProperties(prefix = "educa")
public record EducaProperties(@DefaultValue("media") Path mediaRoot,
                              @DefaultValue("") String siteDomain,
                              @DefaultValue("Educa <noreply@example.com>") String defaultFromEmail,
                              @DefaultValue("false") boolean requireHttps,
                              @DefaultValue List<String> allowedHosts) {
}
