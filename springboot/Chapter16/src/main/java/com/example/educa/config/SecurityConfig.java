package com.example.educa.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

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

    /**
     * API 单独一条过滤器链（@Order(1) 先匹配 /api/**），≈ DRF 视图上的 authentication_classes / permission_classes：
     * <ul>
     *   <li>HTTP Basic 认证，无状态（不创建、不读取会话）——浏览器里登录的会话不能用来调用 API；</li>
     *   <li>既然不用 cookie 认证，CSRF 攻击无从谈起，这条链关闭 CSRF 校验；</li>
     *   <li>读操作公开（≈ DjangoModelPermissionsOrAnonReadOnly），选课和查看内容要求登录。</li>
     * </ul>
     */
    @Bean
    @Order(1)
    @ConditionalOnWebApplication
    SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, e) -> {
            response.setStatus(401);
            response.setHeader("WWW-Authenticate", "Basic realm=\"api\"");
            response.setContentType("application/json");
            String detail = e instanceof BadCredentialsException
                    ? "Invalid username/password."
                    : "Authentication credentials were not provided.";
            response.getWriter().write("{\"detail\": \"" + detail + "\"}");
        };
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/courses/*/enroll/").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/courses/*/contents/").authenticated()
                        .anyRequest().permitAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorized))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    @Order(2)
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
                        .requestMatchers("/course/**", "/students/**", "/chat/**", "/ws/**").authenticated()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .securityContext(context -> context.securityContextRepository(contextRepository))
                // WebSocket 握手没登录时直接返回 401；其余请求照常重定向到登录页（按顺序匹配）
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.withDefaults().matcher("/ws/**"))
                        .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/accounts/login/"),
                                AnyRequestMatcher.INSTANCE))
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
