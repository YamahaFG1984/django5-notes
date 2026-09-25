package com.example.bookmarks.account;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/**
 * 本站用户与第三方账号的绑定关系，对应 python-social-auth 的 social_auth_usersocialauth 表。
 * (provider, uid) 唯一：同一个 Google 账号只能绑定一个本站用户。
 */
@Entity
@Table(name = "social_auth_usersocialauth")
public class UserSocialAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** 第三方的名字，比如 google */
    @Column(nullable = false, length = 32)
    private String provider;

    /** 第三方给的用户唯一标识（OIDC 的 sub），不要用邮箱 —— 邮箱是会变的 */
    @Column(nullable = false, length = 255)
    private String uid;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime created;

    protected UserSocialAuth() {
    }

    public UserSocialAuth(User user, String provider, String uid) {
        this.user = user;
        this.provider = provider;
        this.uid = uid;
    }

    public User getUser() {
        return user;
    }

    public String getProvider() {
        return provider;
    }

    public String getUid() {
        return uid;
    }
}
