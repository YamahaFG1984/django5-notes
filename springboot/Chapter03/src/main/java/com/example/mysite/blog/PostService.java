package com.example.mysite.blog;

import java.util.List;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 读文章列表时需要“顺带”把每篇文章的标签取出来。
 * 我们关闭了 open-in-view，模板渲染时数据库会话已经关了，懒加载会抛 LazyInitializationException，
 * 所以要在事务里把标签初始化好 —— 配合 hibernate.default_batch_fetch_size，
 * 整页文章的标签只用一条 IN (...) 查询取回，效果等同于 Django 的 prefetch_related('tags')。
 */
@Service
@Transactional(readOnly = true)
public class PostService {

    public static final int PAGE_SIZE = 3;

    private final PostRepository posts;

    public PostService(PostRepository posts) {
        this.posts = posts;
    }

    /** 分页列表，tag 为 null 时不过滤；页码越界时回到最后一页。 */
    public Page<Post> publishedPage(Tag tag, int pageNumber) {
        Page<Post> page = fetch(tag, pageNumber);
        if (page.getContent().isEmpty() && page.getTotalPages() > 0) {
            page = fetch(tag, page.getTotalPages());
        }
        page.forEach(post -> Hibernate.initialize(post.getTags()));
        return page;
    }

    public List<Post> similarPosts(Post post, int count) {
        return posts.findSimilar(PostStatus.PUBLISHED, post.getId(), Limit.of(count));
    }

    private Page<Post> fetch(Tag tag, int pageNumber) {
        PageRequest request = PageRequest.of(pageNumber - 1, PAGE_SIZE);
        return tag == null
                ? posts.findPublished(request)
                : posts.findByStatusAndTag(PostStatus.PUBLISHED, tag, request);
    }
}
