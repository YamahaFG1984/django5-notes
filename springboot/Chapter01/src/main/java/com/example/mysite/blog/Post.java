package com.example.mysite.blog;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.mysite.account.User;

/**
 * 博客文章。对照 Django 的 blog/models.py 中的 Post：
 * <ul>
 *   <li>CharField(max_length=250) → {@code @Column(length = 250)}</li>
 *   <li>ForeignKey(User, on_delete=CASCADE) → {@code @ManyToOne} + 迁移里的 ON DELETE CASCADE</li>
 *   <li>auto_now_add / auto_now → {@code @CreationTimestamp} / {@code @UpdateTimestamp}</li>
 *   <li>Meta.indexes → {@code @Table(indexes = ...)}（真正建索引的是 Flyway 脚本）</li>
 *   <li>Meta.ordering 没有直接对应物 —— 排序写在每个查询里</li>
 * </ul>
 */
@Entity
@Table(name = "blog_post", indexes = @Index(name = "blog_post_publish_idx", columnList = "publish DESC"))
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(nullable = false, length = 250)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    /** default=timezone.now：新建对象时取当前时间，之后可以手动修改。 */
    @Column(nullable = false)
    private OffsetDateTime publish = OffsetDateTime.now(ZoneOffset.UTC);

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime created;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updated;

    @Column(nullable = false, length = 2)
    private PostStatus status = PostStatus.DRAFT;

    protected Post() {
    }

    public Post(String title, String slug, User author, String body) {
        this.title = title;
        this.slug = slug;
        this.author = author;
        this.body = body;
    }

    /** Django 的 get_absolute_url()：对象自己知道自己的“规范 URL”。 */
    public String getAbsoluteUrl() {
        return "/blog/" + id + "/";
    }

    public boolean isPublished() {
        return status == PostStatus.PUBLISHED;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public OffsetDateTime getPublish() {
        return publish;
    }

    public void setPublish(OffsetDateTime publish) {
        this.publish = publish;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public OffsetDateTime getUpdated() {
        return updated;
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return title;
    }
}
