package com.example.bookmarks.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 认证配置。对照 Django：
 * <ul>
 *   <li>loginPage("/account/login/") ≈ LOGIN_URL = 'login'</li>
 *   <li>defaultSuccessUrl("/account/") ≈ LOGIN_REDIRECT_URL = 'dashboard'</li>
 *   <li>anyRequest().authenticated() ≈ 给所有视图加 @login_required（Django 5.1 的 LoginRequiredMiddleware）</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    @ConditionalOnWebApplication
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/account/login/", "/account/register/", "/account/logged-out/",
                                "/account/password-reset/**").permitAll()
                        .requestMatchers("/static/**", "/media/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/account/login/")          // GET 由 AuthController 渲染页面，POST 由 Spring Security 处理
                        .defaultSuccessUrl("/account/")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/account/logout/")         // 只接受 POST（开启 CSRF 时的默认行为）
                        .logoutSuccessUrl("/account/logged-out/"));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
