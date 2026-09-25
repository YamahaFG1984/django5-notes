package com.example.educa.courses;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * 模块里的一项内容。Django 版用 GenericForeignKey（content_type + object_id）指向 Text/Video/Image/File 之一；
 * 这里 item 是指向继承体系根类 {@link Item} 的<b>真正的外键</b>，Hibernate 自动实例化正确的子类。
 */
@Entity
@Table(name = "courses_content")
@EntityListeners(OrderFieldListener.class)
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id")
    private Module module;

    /** 一项内容对应一个条目；删除内容时条目一起删除（书中需要手动 content.item.delete()） */
    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "item_id", unique = true)
    private Item item;

    @OrderField(scope = "module")
    @Column(name = "\"order\"", nullable = false)
    private Integer order;

    protected Content() {
    }

    public Content(Module module, Item item) {
        this.module = module;
        this.item = item;
    }

    public Long getId() {
        return id;
    }

    public Module getModule() {
        return module;
    }

    public Item getItem() {
        return item;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
