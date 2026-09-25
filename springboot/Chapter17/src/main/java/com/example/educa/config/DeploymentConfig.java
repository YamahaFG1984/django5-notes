package com.example.educa.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.example.educa.common.web.AllowedHostsFilter;
import com.example.educa.common.web.SubdomainCourseFilter;
import com.example.educa.courses.CourseRepository;

/**
 * 注册两个过滤器，≈ 在 MIDDLEWARE 列表里加中间件。order 越小越靠外（越先执行）。
 * Host 检查放在最外层，比 Spring Security 还早；子域名跳转紧随其后。
 */
@Configuration
public class DeploymentConfig {

    @Bean
    FilterRegistrationBean<AllowedHostsFilter> allowedHostsFilter(EducaProperties properties) {
        var registration = new FilterRegistrationBean<>(new AllowedHostsFilter(properties.allowedHosts()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    FilterRegistrationBean<SubdomainCourseFilter> subdomainCourseFilter(EducaProperties properties,
                                                                       CourseRepository courses) {
        var registration = new FilterRegistrationBean<>(new SubdomainCourseFilter(properties.siteDomain(), courses));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
