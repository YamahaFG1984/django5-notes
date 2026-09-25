package com.example.myshop.i18n;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * URL 语言前缀，≈ i18n_patterns() + LocaleMiddleware：
 * <ul>
 *   <li>/es/carro/ → 语言设为 es，控制器看到的路径是 /cart/</li>
 *   <li>没有前缀的页面（/cart/）→ 按 Accept-Language 重定向到 /en/cart/ 或 /es/carro/</li>
 * </ul>
 * 实现技巧：把 “/es” 当作<b>上下文路径</b>（context path）。Spring MVC、Thymeleaf 的 @{...}、
 * "redirect:" 都会自动在链接前加上下文路径，于是所有生成的链接天然带着语言前缀。
 * 再包装 response.encodeURL()（Servlet 规定所有链接都要经过它），把路径片段翻译成当前语言。
 */
public class LocalePrefixFilter extends OncePerRequestFilter {

    public static final String LANGUAGE_ATTRIBUTE = LocalePrefixFilter.class.getName() + ".language";

    private static final Pattern PREFIX = Pattern.compile("^/(en|es)(/.*)?$");

    /** 不需要语言前缀的路径（≈ 放在 i18n_patterns 之外的 URL，比如 Stripe webhook） */
    private static final List<String> UNPREFIXED = List.of(
            "/static/", "/media/", "/admin", "/payment/webhook/", "/login", "/logout", "/error", "/favicon.ico");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String contextPath = request.getContextPath();
        String path = request.getRequestURI().substring(contextPath.length());

        if (UNPREFIXED.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }
        Matcher matcher = PREFIX.matcher(path);
        if (!matcher.matches()) {
            // 没有语言前缀：重定向（≈ LocaleMiddleware 的行为）
            String language = preferredLanguage(request);
            String target = contextPath + "/" + language + UrlTranslations.toLocal(language, path.isEmpty() ? "/" : path);
            String query = request.getQueryString();
            response.sendRedirect(query == null ? target : target + "?" + query);
            return;
        }
        String language = matcher.group(1);
        String rest = matcher.group(2) == null ? "/" : matcher.group(2);
        String canonical = UrlTranslations.toCanonical(language, rest);
        String languageContext = contextPath + "/" + language;

        request.setAttribute(LANGUAGE_ATTRIBUTE, language);
        chain.doFilter(new PrefixedRequest(request, languageContext, canonical),
                new TranslatingResponse(response, languageContext, language));
    }

    private static String preferredLanguage(HttpServletRequest request) {
        for (Locale locale : java.util.Collections.list(request.getLocales())) {
            if (Languages.isSupported(locale.getLanguage())) {
                return locale.getLanguage();
            }
        }
        return Languages.DEFAULT;
    }

    /** 让下游看到：上下文路径 = /es，应用内路径 = 翻译回英文的 /cart/ */
    private static final class PrefixedRequest extends HttpServletRequestWrapper {

        private final String contextPath;
        private final String pathWithinApplication;

        PrefixedRequest(HttpServletRequest request, String contextPath, String pathWithinApplication) {
            super(request);
            this.contextPath = contextPath;
            this.pathWithinApplication = pathWithinApplication;
        }

        @Override
        public String getContextPath() {
            return contextPath;
        }

        @Override
        public String getRequestURI() {
            return contextPath + pathWithinApplication;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            url.append(getScheme()).append("://").append(getServerName());
            int port = getServerPort();
            if (port > 0 && !(("http".equals(getScheme()) && port == 80) || ("https".equals(getScheme()) && port == 443))) {
                url.append(':').append(port);
            }
            return url.append(getRequestURI());
        }

        @Override
        public String getServletPath() {
            return pathWithinApplication;
        }
    }

    /** 出站链接：/es/cart/ → /es/carro/ */
    private static final class TranslatingResponse extends HttpServletResponseWrapper {

        private final String languageContext;
        private final String language;

        TranslatingResponse(HttpServletResponse response, String languageContext, String language) {
            super(response);
            this.languageContext = languageContext;
            this.language = language;
        }

        @Override
        public String encodeURL(String url) {
            return super.encodeURL(translate(url));
        }

        @Override
        public String encodeRedirectURL(String url) {
            return super.encodeRedirectURL(translate(url));
        }

        private String translate(String url) {
            try {
                URI uri = URI.create(url);
                String path = uri.getRawPath();
                if (path == null || !path.startsWith(languageContext + "/")) {
                    return url;
                }
                String translated = languageContext + UrlTranslations.toLocal(language, path.substring(languageContext.length()));
                return url.replaceFirst(Pattern.quote(path), Matcher.quoteReplacement(translated));
            } catch (IllegalArgumentException e) {
                return url;
            }
        }
    }
}
