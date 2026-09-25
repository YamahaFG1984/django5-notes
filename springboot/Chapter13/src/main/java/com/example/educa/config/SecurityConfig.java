package com.example.educa.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 两层保护：
 * <ul>
 *   <li>URL 层：/course/** 必须登录（≈ LoginRequiredMixin）；</li>
 *   <li>方法层：@EnableMethodSecurity 打开 @PreAuthorize，控制器方法上写
 *       hasAuthority('courses.add_course')（≈ PermissionRequiredMixin + permission_required）。</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnWebApplication
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/course/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form.loginPage("/accounts/login/").defaultSuccessUrl("/course/mine/").permitAll())
                .logout(logout -> logout.logoutUrl("/accounts/logout/").logoutSuccessUrl("/accounts/logged-out/"));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
