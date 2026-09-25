package com.example.bookmarks.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.bookmarks.account.EmailUserDetailsService;
import com.example.bookmarks.account.JpaUserDetailsService;
import com.example.bookmarks.account.SocialUserService;

@Configuration
public class SecurityConfig {

    /**
     * 两个认证提供者按顺序尝试，第一个成功的生效 —— 这正是 Django 的
     * <pre>
     * AUTHENTICATION_BACKENDS = [
     *     'django.contrib.auth.backends.ModelBackend',      # 用户名 + 密码
     *     'account.authentication.EmailAuthBackend',        # 邮箱 + 密码
     *     'social_core.backends.google.GoogleOAuth2',       # 见下面的 oauth2Login
     * ]
     * </pre>
     */
    @Bean
    AuthenticationManager authenticationManager(JpaUserDetailsService byUsername,
                                                EmailUserDetailsService byEmail,
                                                PasswordEncoder passwordEncoder,
                                                AuthenticationEventPublisher eventPublisher) {
        DaoAuthenticationProvider usernameProvider = new DaoAuthenticationProvider(byUsername);
        usernameProvider.setPasswordEncoder(passwordEncoder);
        DaoAuthenticationProvider emailProvider = new DaoAuthenticationProvider(byEmail);
        emailProvider.setPasswordEncoder(passwordEncoder);
        ProviderManager manager = new ProviderManager(usernameProvider, emailProvider);
        manager.setAuthenticationEventPublisher(eventPublisher);   // 登录成功事件 → LastLoginListener
        return manager;
    }

    @Bean
    @ConditionalOnWebApplication
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager,
                                            SocialUserService socialUserService) throws Exception {
        http
                .authenticationManager(authenticationManager)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/account/login/", "/account/register/", "/account/logged-out/",
                                "/account/password-reset/**").permitAll()
                        .requestMatchers("/static/**", "/media/**", "/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")   // 运维端点只给超级用户看
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/account/login/")
                        .defaultSuccessUrl("/account/")
                        .permitAll())
                // 社交登录：/oauth2/authorization/google → Google → /login/oauth2/code/google
                .oauth2Login(oauth -> oauth
                        .loginPage("/account/login/")
                        .defaultSuccessUrl("/account/")
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(socialUserService)))
                .logout(logout -> logout
                        .logoutUrl("/account/logout/")
                        .logoutSuccessUrl("/account/logged-out/"));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
