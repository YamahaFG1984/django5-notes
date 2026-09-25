package com.example.mysite.blog;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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

    /** unique_for_date='publish'：同一天内 slug 不能重复。Django 只在表单层校验，这里也一样（见 PostAdminController）。 */
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

    /** tags = TaggableManager()：多对多，中间表 blog_post_tags。 */
    @ManyToMany
    @JoinTable(name = "blog_post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @OrderBy("name")
    private Set<Tag> tags = new LinkedHashSet<>();

    /** 反向关系，相当于 ForeignKey(related_name='comments') 让你能写 post.comments。 */
    @OneToMany(mappedBy = "post")
    private List<Comment> comments = new ArrayList<>();

    protected Post() {
    }

    public Post(String title, String slug, User author, String body) {
        this.title = title;
        this.slug = slug;
        this.author = author;
        this.body = body;
    }

    /**
     * 规范 URL：/blog/2025/1/31/who-was-django-reinhardt/，
     * 对应 reverse('blog:post_detail', args=[year, month, day, slug])。日期按 UTC 计算。
     */
    public String getAbsoluteUrl() {
        OffsetDateTime utc = publish.withOffsetSameInstant(ZoneOffset.UTC);
        return "/blog/%d/%d/%d/%s/".formatted(utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth(), slug);
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

    public List<Comment> getComments() {
        return comments;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return title;
    }
}
