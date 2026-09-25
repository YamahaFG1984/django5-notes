package com.example.mysite.blog;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.example.mysite.account.User;

/** 用户收藏的文章，主键是 (user, post) 组合，天然保证“同一篇只能收藏一次”。 */
@Entity
@Table(name = "blog_favouritepost")
public class FavouritePost {

    @EmbeddedId
    private FavouritePostId id;

    /** @MapsId：主键里的 userId 就取自这个外键，两者是同一列。 */
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime created;

    protected FavouritePost() {
    }

    public FavouritePost(User user, Post post) {
        this.id = new FavouritePostId(user.getId(), post.getId());
        this.user = user;
        this.post = post;
    }

    public FavouritePostId getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Post getPost() {
        return post;
    }

    public OffsetDateTime getCreated() {
        return created;
    }
}
