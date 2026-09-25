package com.example.mysite.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 纯单元测试：不启动 Spring，毫秒级完成（≈ 用 SimpleTestCase 测试一个模板过滤器）。 */
class TextFiltersTests {

    private final TextFilters filters = new TextFilters();
    private final MarkdownRenderer markdown = new MarkdownRenderer();

    @Test
    void truncatewordsHtmlKeepsTagsBalanced() {
        String html = "<p>one <strong>two three</strong> four</p><p>five six</p>";
        assertThat(filters.truncatewordsHtml(html, 2)).isEqualTo("<p>one <strong>two …</strong></p>");
        assertThat(filters.truncatewordsHtml(html, 4)).isEqualTo("<p>one <strong>two three</strong> four …</p>");
        assertThat(filters.truncatewordsHtml(html, 6)).isEqualTo(html);
        assertThat(filters.truncatewordsHtml(html, 10)).isEqualTo(html);
    }

    @Test
    void markdownEscapesRawHtmlAndUnsafeLinks() {
        assertThat(markdown.render("**bold** and *em*")).isEqualTo("<p><strong>bold</strong> and <em>em</em></p>\n");
        assertThat(markdown.render("<script>alert(1)</script>")).doesNotContain("<script>");
        assertThat(markdown.render("[x](javascript:alert(1))")).doesNotContain("javascript:");
    }

    @Test
    void slugify() {
        assertThat(Slugs.slugify("Who was Django Reinhardt?")).isEqualTo("who-was-django-reinhardt");
        assertThat(Slugs.slugify("  Crème brûlée  ")).isEqualTo("creme-brulee");
    }
}
