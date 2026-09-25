package com.example.educa.common;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Django 内置模板过滤器 linebreaks / truncatewords 的替代品。
 * Thymeleaf 没有“过滤器”概念，这里写成普通方法，再由 {@link TextDialect} 暴露为 {@code #text}。
 */
@Component
public class TextFilters {

    /** {{ value|linebreaks }}：先转义 HTML，空行分段成 &lt;p&gt;，单个换行变 &lt;br&gt;。 */
    public String linebreaks(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n').strip();
        return Arrays.stream(normalized.split("\n{2,}"))
                .map(paragraph -> "<p>" + HtmlUtils.htmlEscape(paragraph).replace("\n", "<br>") + "</p>")
                .collect(Collectors.joining("\n\n"));
    }

    /** {{ value|truncatewords:n }}：保留前 n 个单词，超出部分用 … 结尾。 */
    public String truncatewords(String value, int count) {
        if (value == null) {
            return "";
        }
        String[] words = value.strip().split("\\s+");
        if (words.length <= count) {
            return value.strip();
        }
        return String.join(" ", Arrays.copyOfRange(words, 0, count)) + " …";
    }
}
