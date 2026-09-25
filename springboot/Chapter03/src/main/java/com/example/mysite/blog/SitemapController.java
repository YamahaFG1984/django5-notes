package com.example.mysite.blog;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.HtmlUtils;

/**
 * /sitemap.xml，对应 blog/sitemaps.py 的 PostSitemap（changefreq='weekly'、priority=0.9、lastmod=updated），
 * 另外按仓库 prompts/task.md 的思路加上了每个标签页。
 * 区别：Django 的 sitemap 框架用 Sites 框架里配置的域名（默认 example.com），这里直接用当前请求的域名。
 */
@RestController
public class SitemapController {

    private final PostRepository posts;
    private final TagRepository tags;

    public SitemapController(PostRepository posts, TagRepository tags) {
        this.posts = posts;
        this.tags = tags;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);
        for (Post post : posts.findByStatusOrderByUpdatedDesc(PostStatus.PUBLISHED)) {
            String lastmod = post.getUpdated().withOffsetSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE);
            url(xml, baseUrl + post.getAbsoluteUrl(), lastmod, "weekly", "0.9");
        }
        for (Tag tag : tags.findAll()) {
            url(xml, baseUrl + "/blog/tag/" + tag.getSlug() + "/", null, "weekly", "0.6");
        }
        return xml.append("</urlset>\n").toString();
    }

    private static void url(StringBuilder xml, String loc, String lastmod, String changefreq, String priority) {
        xml.append("  <url>\n    <loc>").append(HtmlUtils.htmlEscape(loc)).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n")
                .append("    <priority>").append(priority).append("</priority>\n  </url>\n");
    }
}
