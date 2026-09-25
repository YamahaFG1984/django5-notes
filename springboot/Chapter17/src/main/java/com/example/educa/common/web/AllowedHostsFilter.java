package com.example.educa.common.web;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ≈ Django 的 ALLOWED_HOSTS 检查：Host 头不在白名单里的请求直接返回 400。
 * <p>
 * Spring Boot 默认不检查 Host 头。伪造的 Host 会进入应用生成的绝对地址（比如 API 分页里的 next 链接、
 * 邮件里的链接），这就是“Host 头注入”。在 Nginx 里只接受 server_name 列出的域名也能达到同样效果，两层都做更稳妥。
 */
public class AllowedHostsFilter extends OncePerRequestFilter {

    private final List<String> allowedHosts;

    public AllowedHostsFilter(List<String> allowedHosts) {
        this.allowedHosts = allowedHosts.stream().map(h -> h.strip().toLowerCase(Locale.ROOT)).toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (allowedHosts.isEmpty() || isAllowed(request.getServerName())) {
            chain.doFilter(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid HTTP_HOST header");
        }
    }

    boolean isAllowed(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        for (String pattern : allowedHosts) {
            if (pattern.equals("*") || pattern.equals(h)) {
                return true;
            }
            // '.example.com' 匹配 example.com 以及它的所有子域名
            if (pattern.startsWith(".") && (h.equals(pattern.substring(1)) || h.endsWith(pattern))) {
                return true;
            }
        }
        return false;
    }
}
