package com.example.myshop.i18n;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

/**
 * 国际化配置（≈ settings 里的 LANGUAGES、LocaleMiddleware 以及 urls.py 里的 i18n_patterns）。
 * 翻译文本本身由 Spring Boot 自动配置的 MessageSource 从 i18n/messages*.properties 读取。
 */
@Configuration
public class I18nConfig {

    /** Bean 名字必须是 localeResolver，DispatcherServlet 按名字找它 */
    @Bean
    LocaleResolver localeResolver() {
        return new PrefixLocaleResolver();
    }

    /**
     * 排在 Spring Security 过滤器链（order = -100）<b>之后</b>。
     * 原因：Security 默认的 DisableEncodeUrlFilter 会把 response.encodeURL() 包成“原样返回”（防止 jsessionid 进入 URL），
     * 如果我们的过滤器在它前面，我们对 encodeURL 的包装就被它挡住了，出站链接不会被翻译。
     * 放在后面，我们的包装在最外层，先翻译，再交给它。代价是安全规则看到的是带前缀的原始路径 ——
     * 本项目需要权限的只有不带前缀的 /admin/**，所以没有影响。
     */
    @Bean
    FilterRegistrationBean<LocalePrefixFilter> localePrefixFilter() {
        FilterRegistrationBean<LocalePrefixFilter> registration = new FilterRegistrationBean<>(new LocalePrefixFilter());
        registration.setOrder(0);
        return registration;
    }
}
