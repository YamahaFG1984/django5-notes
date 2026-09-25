package com.example.educa.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * 两层保护：
 * <ul>
 *   <li>URL 层：哪些地址必须登录（≈ LoginRequiredMixin）；规则从上到下，第一条匹配的生效；</li>
 *   <li>方法层：@EnableMethodSecurity 打开 @PreAuthorize，控制器方法上写
 *       hasAuthority('courses.add_course')（≈ PermissionRequiredMixin + permission_required）。</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnWebApplication
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contextRepository)
            throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 公开目录：学科筛选、课程详情（/course/<slug>/ 只有一段）
                        .requestMatchers("/course/subject/**").permitAll()
                        .requestMatchers("/course/mine/", "/course/create/").authenticated()
                        .requestMatchers("/course/*/").permitAll()
                        .requestMatchers("/students/register/").permitAll()
                        .requestMatchers("/course/**", "/students/**").authenticated()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .securityContext(context -> context.securityContextRepository(contextRepository))
                // LOGIN_REDIRECT_URL = reverse_lazy('student_course_list')
                .formLogin(form -> form.loginPage("/accounts/login/").defaultSuccessUrl("/students/courses/").permitAll())
                .logout(logout -> logout.logoutUrl("/accounts/logout/").logoutSuccessUrl("/accounts/logged-out/"));
        return http.build();
    }

    /** 与 Spring Security 默认的仓库相同；声明成 Bean，注册后自动登录时要用它把登录状态存进会话 */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(), new HttpSessionSecurityContextRepository());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
