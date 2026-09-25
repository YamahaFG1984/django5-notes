package com.example.myshop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** /media/** → 本地上传目录（≈ 开发时的 static(settings.MEDIA_URL, ...)）。 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final MyshopProperties properties;

    public WebConfig(MyshopProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(properties.mediaRoot().toAbsolutePath().normalize().toUri().toString());
    }
}
