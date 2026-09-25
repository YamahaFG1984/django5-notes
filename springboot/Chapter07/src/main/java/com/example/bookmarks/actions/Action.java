package com.example.bookmarks.actions;

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

import org.hibernate.annotations.CreationTimestamp;

import com.example.bookmarks.account.User;

/**
 * 用户动态，对应 actions/models.py 的 Action：
 * <pre>
 * target_ct = ForeignKey(ContentType, ...)   →  targetType（字符串）
 * target_id = PositiveIntegerField(...)      →  targetId
 * target    = GenericForeignKey(...)         →  由 ActionService 按类型批量加载
 * </pre>
 */
@Entity
@Table(name = "actions_action", indexes = {
        @Index(name = "actions_action_created_idx", columnList = "created DESC"),
        @Index(name = "actions_action_target_idx", columnList = "target_type, target_id")})
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** related_name='actions' */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 255)
    private String verb;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime created;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    protected Action() {
    }

    public Action(User user, String verb, ActionTarget target) {
        this.user = user;
        this.verb = verb;
        if (target != null) {
            this.targetType = target.targetType();
            this.targetId = target.getId();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getVerb() {
        return verb;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }
}
