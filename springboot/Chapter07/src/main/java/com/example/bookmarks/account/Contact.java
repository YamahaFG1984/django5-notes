package com.example.bookmarks.account;

import java.time.OffsetDateTime;

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
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;

/**
 * 关注关系：userFrom 关注了 userTo。对应 Django 的 Contact 模型，它是
 * {@code User.following = ManyToManyField('self', through=Contact, symmetrical=False)} 的“中间模型”。
 * JPA 里带额外字段（created）的多对多，通常就直接建模成这样一个实体。
 */
@Entity
@Table(name = "account_contact",
        indexes = @Index(name = "account_contact_created_idx", columnList = "created DESC"),
        uniqueConstraints = @UniqueConstraint(name = "account_contact_from_to_uniq", columnNames = {"user_from_id", "user_to_id"}))
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** related_name='rel_from_set' */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_from_id")
    private User userFrom;

    /** related_name='rel_to_set' */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_to_id")
    private User userTo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime created;

    protected Contact() {
    }

    public Contact(User userFrom, User userTo) {
        this.userFrom = userFrom;
        this.userTo = userTo;
    }

    public User getUserFrom() {
        return userFrom;
    }

    public User getUserTo() {
        return userTo;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    @Override
    public String toString() {
        return userFrom + " follows " + userTo;
    }
}
