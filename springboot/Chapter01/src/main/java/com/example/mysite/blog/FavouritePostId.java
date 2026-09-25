package com.example.mysite.blog;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 复合主键 (user_id, post_id)。
 * Django 5.2 写 {@code pk = models.CompositePrimaryKey("user", "post")}；
 * JPA 早就支持复合主键，用一个 @Embeddable 类（这里用 record）表示。
 */
@Embeddable
public record FavouritePostId(
        @Column(name = "user_id") Long userId,
        @Column(name = "post_id") Long postId) implements Serializable {
}
