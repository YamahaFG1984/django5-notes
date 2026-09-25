package com.example.mysite.blog.templatetags;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import com.example.mysite.blog.Post;
import com.example.mysite.blog.PostRepository;
import com.example.mysite.blog.PostStatus;
import com.example.mysite.common.MarkdownRenderer;

/**
 * blog/templatetags/blog_tags.py 的 Java 版。模板里通过 {@code #blog} 调用（见 {@link BlogDialect}）：
 * <pre>
 * {% total_posts %}                              → ${#blog.totalPosts()}
 * {% show_latest_posts 3 %}                      → th:replace="~{blog/post/latest_posts :: latestPosts(${#blog.latestPosts(3)})}"
 * {% get_most_commented_posts as most_commented %} → th:with="mostCommented=${#blog.mostCommentedPosts(5)}"
 * {{ post.body|markdown }}                       → th:utext="${#blog.markdown(post.body)}"
 * </pre>
 */
@Component
public class BlogTags {

    private final PostRepository posts;
    private final MarkdownRenderer markdownRenderer;

    public BlogTags(PostRepository posts, MarkdownRenderer markdownRenderer) {
        this.posts = posts;
        this.markdownRenderer = markdownRenderer;
    }

    /** @register.simple_tag def total_posts() */
    public long totalPosts() {
        return posts.countByStatus(PostStatus.PUBLISHED);
    }

    /** @register.inclusion_tag('blog/post/latest_posts.html') 的数据部分；HTML 部分是一个 Thymeleaf 片段 */
    public List<Post> latestPosts(int count) {
        return posts.findByStatusOrderByPublishDesc(PostStatus.PUBLISHED, Limit.of(count));
    }

    /** @register.simple_tag def get_most_commented_posts(count=5) */
    public List<Post> mostCommentedPosts(int count) {
        return posts.findMostCommented(PostStatus.PUBLISHED, Limit.of(count));
    }

    /** @register.filter(name='markdown') */
    public String markdown(String text) {
        return markdownRenderer.render(text);
    }
}
