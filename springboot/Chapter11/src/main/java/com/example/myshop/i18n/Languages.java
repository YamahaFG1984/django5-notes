package com.example.myshop.i18n;

import java.util.List;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 站点支持的语言，≈ settings.LANGUAGES + LANGUAGE_CODE。
 * {@link #current()} 返回“当前激活的语言”，≈ django.utils.translation.get_language()：
 * Spring 在处理每个请求时把 LocaleResolver 解析出的语言放进线程绑定的 LocaleContextHolder。
 */
public final class Languages {

    public static final String DEFAULT = "en";

    /** code + 本地名称（≈ language.name_local） */
    public record Language(String code, String nameLocal) {
    }

    public static final List<Language> ALL = List.of(
            new Language("en", "English"),
            new Language("es", "español"));

    private Languages() {
    }

    public static boolean isSupported(String code) {
        return ALL.stream().anyMatch(l -> l.code().equals(code));
    }

    public static String current() {
        String language = LocaleContextHolder.getLocale().getLanguage();
        return isSupported(language) ? language : DEFAULT;
    }

    public static Locale locale(String code) {
        return Locale.forLanguageTag(code);
    }
}
