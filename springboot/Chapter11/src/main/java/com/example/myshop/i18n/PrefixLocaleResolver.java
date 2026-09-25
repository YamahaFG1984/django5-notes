package com.example.myshop.i18n;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.LocaleResolver;

/**
 * 语言从 URL 前缀来（LocalePrefixFilter 放进请求属性），没有前缀的请求（比如后台）用默认语言。
 * Spring 自带 AcceptHeaderLocaleResolver、CookieLocaleResolver、SessionLocaleResolver，但没有“URL 前缀”这种。
 */
public class PrefixLocaleResolver implements LocaleResolver {

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        Object language = request.getAttribute(LocalePrefixFilter.LANGUAGE_ATTRIBUTE);
        return Languages.locale(language instanceof String code ? code : Languages.DEFAULT);
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        throw new UnsupportedOperationException("语言由 URL 前缀决定，请跳转到 /" + locale.getLanguage() + "/ 下的地址");
    }
}
