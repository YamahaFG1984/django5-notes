package com.example.mysite.blog;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.mysite.common.MarkdownRenderer;
import com.example.mysite.common.TextFilters;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

/**
 * RSS 订阅源，对应 blog/feeds.py 的 LatestPostsFeed。
 * Django 的 Feed 类用 title/link/items()/item_title() 等“钩子”声明订阅源；
 * 这里用 ROME 库直接构造 SyndFeed 对象，再输出成 RSS 2.0 XML。
 */
@Controller
public class LatestPostsFeedController {

    private final PostRepository posts;
    private final MarkdownRenderer markdown;
    private final TextFilters text;

    public LatestPostsFeedController(PostRepository posts, MarkdownRenderer markdown, TextFilters text) {
        this.posts = posts;
        this.markdown = markdown;
        this.text = text;
    }

    @GetMapping("/blog/feed/")
    public void feed(HttpServletResponse response) throws IOException, FeedException {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();

        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        feed.setTitle("My blog");
        feed.setLink(baseUrl + "/blog/");
        feed.setDescription("New posts of my blog.");

        List<Post> latest = posts.findByStatusOrderByPublishDesc(PostStatus.PUBLISHED, Limit.of(5));
        feed.setEntries(latest.stream().map(post -> {
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(post.getTitle());                    // item_title
            entry.setLink(baseUrl + post.getAbsoluteUrl());     // item_link = get_absolute_url
            entry.setPublishedDate(Date.from(post.getPublish().toInstant()));   // item_pubdate
            SyndContentImpl description = new SyndContentImpl();
            description.setType("text/html");
            description.setValue(text.truncatewordsHtml(markdown.render(post.getBody()), 30));   // item_description
            entry.setDescription(description);
            return entry;
        }).toList());

        response.setContentType("application/rss+xml;charset=utf-8");
        try (Writer writer = response.getWriter()) {
            new SyndFeedOutput().output(feed, writer);
        }
    }
}
