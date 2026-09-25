package com.example.mysite.common;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

/**
 * Markdown → HTML，基于 commonmark-java。
 * 和书中 mark_safe(markdown.markdown(text)) 的区别：这里 escapeHtml(true) 会把正文里的原始 HTML 转义掉，
 * sanitizeUrls(true) 会过滤 javascript: 链接，避免作者（或被盗的作者账号）在文章里注入脚本。
 */
@Component
public class MarkdownRenderer {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    public String render(String markdown) {
        if (markdown == null) {
            return "";
        }
        return renderer.render(parser.parse(markdown));
    }
}
