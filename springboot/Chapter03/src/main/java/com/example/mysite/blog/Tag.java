package com.example.mysite.blog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.example.mysite.common.Slugs;

/**
 * 标签。Django 书里直接用第三方包 django-taggit（TaggableManager），
 * Java 生态没有同等的“即插即用”标签库，自己建一张表 + 多对多关系即可。
 */
@Entity
@Table(name = "blog_tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    protected Tag() {
    }

    public Tag(String name) {
        this.name = name;
        this.slug = Slugs.slugify(name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    @Override
    public String toString() {
        return name;
    }
}
