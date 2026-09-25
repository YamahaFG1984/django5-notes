package com.example.educa.common.web;

import java.io.IOException;
import java.util.Locale;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.educa.courses.CourseRepository;

/**
 * 课程子域名：django.educaproject.com → https://educaproject.com/course/django/，
 * ≈ courses/middleware.py 的 subdomain_course_middleware。
 * <p>
 * Django 的“中间件”在 Spring 里对应 Servlet 过滤器：包在所有请求外面，可以直接返回响应，也可以交给后面的链继续处理。
 * <p>
 * 书中的判断是“Host 按点拆开多于两段，且第一段不是 www”，这会把 127.0.0.1、192.168.1.10 这类 IP 地址
 * 也当成子域名（第一段 '127' 被当作课程 slug，结果所有请求都 404）。这里只处理配置的主域名下的子域名。
 */
public class SubdomainCourseFilter extends OncePerRequestFilter {

    private final String siteDomain;
    private final CourseRepository courses;

    public SubdomainCourseFilter(String siteDomain, CourseRepository courses) {
        this.siteDomain = siteDomain.toLowerCase(Locale.ROOT);
        this.courses = courses;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String subdomain = subdomainOf(request.getServerName());
        if (subdomain == null) {
            chain.doFilter(request, response);
            return;
        }
        // get_object_or_404(Course, slug=host_parts[0])
        if (courses.findBySlug(subdomain).isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String url = UriComponentsBuilder.newInstance()
                .scheme(request.getScheme())
                .host(siteDomain)
                .port(isDefaultPort(request) ? -1 : request.getServerPort())
                .path("/course/{slug}/")
                .buildAndExpand(subdomain)
                .toUriString();
        response.sendRedirect(url);
    }

    /** 返回 'django'（django.educaproject.com）；主域名本身、www 和不相关的主机名返回 null */
    String subdomainOf(String host) {
        if (siteDomain.isEmpty()) {
            return null;
        }
        String h = host.toLowerCase(Locale.ROOT);
        String suffix = "." + siteDomain;
        if (!h.endsWith(suffix)) {
            return null;
        }
        String sub = h.substring(0, h.length() - suffix.length());
        return sub.isEmpty() || sub.equals("www") || sub.contains(".") ? null : sub;
    }

    private static boolean isDefaultPort(HttpServletRequest request) {
        int port = request.getServerPort();
        return port == 80 && "http".equals(request.getScheme()) || port == 443 && "https".equals(request.getScheme());
    }
}
