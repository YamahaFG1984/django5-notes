package com.example.myshop.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 商店前台对所有人开放（购物车存在匿名会话里），只有后台需要 staff 登录。
 * CSRF 保护照常开启：加入购物车、下单都是 POST 表单。
 */
@Configuration
public class SecurityConfig {

    @Bean
    @ConditionalOnWebApplication
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("STAFF")
                        .anyRequest().permitAll())
                // Stripe 的服务器调用 webhook 时不可能带 CSRF 令牌 —— 靠签名校验保证来源（≈ @csrf_exempt）
                .csrf(csrf -> csrf.ignoringRequestMatchers("/payment/webhook/"))
                .formLogin(Customizer.withDefaults())
                .logout(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
