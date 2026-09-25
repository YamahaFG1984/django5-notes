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

/** 商品的某种语言的译文：name、slug、description 三个可翻译字段。 */
@Entity
@Table(name = "shop_product_translation", uniqueConstraints =
        @UniqueConstraint(name = "shop_product_translation_lang_master_uniq", columnNames = {"language_code", "master_id"}))
public class ProductTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_id")
    private Product master;

    @Column(name = "language_code", nullable = false, length = 15)
    private String languageCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String slug;

    @Column(nullable = false, columnDefinition = "text")
    private String description = "";

    protected ProductTranslation() {
    }

    ProductTranslation(Product master, String languageCode, String name, String slug, String description) {
        this.master = master;
        this.languageCode = languageCode;
        update(name, slug, description);
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

    public String getDescription() {
        return description;
    }

    void update(String name, String slug, String description) {
        this.name = name;
        this.slug = slug;
        this.description = description == null ? "" : description;
    }
}
