package com.example.bookmarks.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层的小配置。
 * /media/** 映射到本地上传目录，相当于 Django 开发时在 urls.py 里加的
 * {@code static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)}。
 * 生产环境应该让 Nginx 或对象存储直接提供这些文件（第 17 章）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final BookmarksProperties properties;

    public WebConfig(BookmarksProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = properties.mediaRoot().toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/media/**").addResourceLocations(location);
    }

    /** 只渲染模板、没有逻辑的页面，不必写控制器（≈ Django 的 TemplateView）。 */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/account/");
        registry.addViewController("/account/logged-out/").setViewName("registration/logged_out");
        registry.addViewController("/account/password-change/done/").setViewName("registration/password_change_done");
        registry.addViewController("/account/password-reset/done/").setViewName("registration/password_reset_done");
        registry.addViewController("/account/password-reset/complete/").setViewName("registration/password_reset_complete");
    }
}
