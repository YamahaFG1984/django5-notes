package com.example.myshop.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 分类的某种语言的译文。django-parler 为每个 TranslatableModel 自动生成一张 xxx_translation 表，
 * 结构正是这样：master_id + language_code + 可翻译字段。
 */
@Entity
@Table(name = "shop_category_translation", uniqueConstraints = {
        @UniqueConstraint(name = "shop_category_translation_lang_master_uniq", columnNames = {"language_code", "master_id"}),
        @UniqueConstraint(name = "shop_category_translation_lang_slug_uniq", columnNames = {"language_code", "slug"})})
public class CategoryTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_id")
    private Category master;

    @Column(name = "language_code", nullable = false, length = 15)
    private String languageCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String slug;

    protected CategoryTranslation() {
    }

    CategoryTranslation(Category master, String languageCode, String name, String slug) {
        this.master = master;
        this.languageCode = languageCode;
        this.name = name;
        this.slug = slug;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    void update(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }
}
