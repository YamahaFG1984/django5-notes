package com.example.educa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final EducaProperties properties;

    public WebConfig(EducaProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(properties.mediaRoot().toAbsolutePath().normalize().toUri().toString());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 本章还没有公开的首页，先把 / 指向“我的课程”
        registry.addRedirectViewController("/", "/course/mine/");
        registry.addViewController("/accounts/login/").setViewName("registration/login");
        registry.addViewController("/accounts/logged-out/").setViewName("registration/logged_out");
    }
}
