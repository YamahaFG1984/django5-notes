package com.example.bookmarks.images;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.example.bookmarks.account.User;
import com.example.bookmarks.actions.ActionTarget;
import com.example.bookmarks.common.Slugs;

/**
 * 用户收藏的图片，对应 images/models.py 的 Image。
 * 两个指向 User 的关系：user（谁收藏的，多对一）和 usersLike（谁点了赞，多对多）。
 */
@Entity
@Table(name = "images_image", indexes = {
        @Index(name = "images_image_created_idx", columnList = "created DESC"),
        @Index(name = "images_image_total_likes_idx", columnList = "total_likes DESC")})
public class Image implements ActionTarget {

    public static final String TARGET_TYPE = "image";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** related_name='images_created' */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String slug = "";

    /** 图片原始地址 */
    @Column(nullable = false, length = 2000)
    private String url;

    /** 下载后保存在 MEDIA_ROOT 下的相对路径，≈ ImageField(upload_to='images/%Y/%m/%d/') */
    @Column(nullable = false, length = 100)
    private String image;

    @Column(nullable = false, columnDefinition = "text")
    private String description = "";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime created;

    /** users_like = ManyToManyField(User, related_name='images_liked', blank=True) */
    @ManyToMany
    @JoinTable(name = "images_image_users_like",
            joinColumns = @JoinColumn(name = "image_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> usersLike = new LinkedHashSet<>();

    /**
     * 点赞数的冗余字段（反范式）：列表按热度排序时不必每次 COUNT 中间表。
     * 由 ImageLikesListener 在点赞变化时维护（≈ 书中 m2m_changed 信号的接收者）。
     */
    @Column(name = "total_likes", nullable = false)
    private int totalLikes;

    protected Image() {
    }

    public Image(User user, String title, String url, String image, String description) {
        this.user = user;
        this.title = title;
        this.url = url;
        this.image = image;
        this.description = description == null ? "" : description;
    }

    /**
     * 相当于重写 save()：保存前如果没有 slug，就根据标题生成。
     * JPA 的生命周期回调（@PrePersist、@PreUpdate…）就是做这类事的地方。
     */
    @PrePersist
    void prePersist() {
        if (slug == null || slug.isBlank()) {
            slug = Slugs.slugify(title);
        }
    }

    /** reverse('images:detail', args=[self.id, self.slug]) */
    public String getAbsoluteUrl() {
        return "/images/detail/" + id + "/" + slug + "/";
    }

    public String getImageUrl() {
        return "/media/" + image;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getUrl() {
        return url;
    }

    public String getImage() {
        return image;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public Set<User> getUsersLike() {
        return usersLike;
    }

    public int getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(int totalLikes) {
        this.totalLikes = totalLikes;
    }

    @Override
    public String targetType() {
        return TARGET_TYPE;
    }

    @Override
    public String toString() {
        return title;
    }
}
