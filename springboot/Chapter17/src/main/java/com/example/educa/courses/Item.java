package com.example.educa.courses;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.educa.account.User;

/**
 * 内容条目的公共基类，≈ Django 的抽象模型 ItemBase（owner、title、created、updated）。
 * <p>
 * JPA 的继承策略有三种：
 * <ul>
 *   <li>{@code @MappedSuperclass}：只继承字段，没有公共表 ≈ Django 的 abstract = True（书中用的）</li>
 *   <li>{@code JOINED}：公共字段一张表，子类各一张表，按 id 关联 ≈ Django 的多表继承（本项目用的）</li>
 *   <li>{@code SINGLE_TABLE}：所有子类挤一张表，用类型列区分</li>
 * </ul>
 * 选 JOINED 是因为 Content 需要“指向任意一种条目”：有了公共表 courses_item，就能建真正的外键，
 * 不必像 Django 那样借助 contenttypes 的泛型外键。
 */
@Entity
@Table(name = "courses_item")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "item_type", length = 20)
public abstract class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** related_name='%(class)s_related' */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false, length = 250)
    private String title;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime created;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updated;

    protected Item() {
    }

    protected Item(User owner, String title) {
        this.owner = owner;
        this.title = title;
    }

    /** ≈ item._meta.model_name（书中写了一个模板过滤器 model_name 来取它） */
    public abstract String getModelName();

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public OffsetDateTime getUpdated() {
        return updated;
    }

    @Override
    public String toString() {
        return title;
    }
}
