package com.example.mysite.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置。Django 里这些分散在好几处：
 * MIDDLEWARE 里的 CsrfViewMiddleware / AuthenticationMiddleware、视图上的 @login_required、
 * admin 站点对 is_staff 的检查。Spring Security 用一条过滤器链集中声明。
 */
@Configuration
public class SecurityConfig {

    /** 只在 Web 模式下需要；以命令行模式运行 createsuperuser 时跳过。 */
    @Bean
    @ConditionalOnWebApplication
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("STAFF")          // admin 只允许 is_staff
                        .requestMatchers("/blog/favourite/**", "/blog/favourites/**").authenticated() // @login_required
                        .anyRequest().permitAll())
                .formLogin(Customizer.withDefaults())   // 先用 Spring Security 自带的登录页，第 4 章再自己写
                .logout(Customizer.withDefaults());
        // CSRF 保护默认开启，相当于 CsrfViewMiddleware；Thymeleaf 的 th:action 表单会自动带上 token
        return http.build();
    }

    /** 带算法前缀的密码哈希（默认 bcrypt），相当于 Django 的 PASSWORD_HASHERS。 */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
