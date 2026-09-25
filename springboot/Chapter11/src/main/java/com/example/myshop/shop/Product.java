package com.example.myshop.shop;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.myshop.i18n.Languages;

/**
 * 可翻译的商品（≈ class Product(TranslatableModel)，name、slug、description 放进 TranslatedFields）。
 * 价格、图片、上架状态等不需要翻译的字段仍在主表。
 */
@Entity
@Table(name = "shop_product", indexes = @Index(name = "shop_product_created_idx", columnList = "created DESC"))
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** related_name='products' */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    /** ImageField(upload_to='products/%Y/%m/%d', blank=True)：空字符串表示没有图片 */
    @Column(nullable = false, length = 100)
    private String image = "";

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean available = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime created;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updated;

    @OneToMany(mappedBy = "master", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKey(name = "languageCode")
    @Fetch(FetchMode.SUBSELECT)
    private Map<String, ProductTranslation> translations = new HashMap<>();

    protected Product() {
    }

    public Product(Category category, String name, String slug, BigDecimal price) {
        this.category = category;
        this.price = price;
        translate(Languages.DEFAULT, name, slug, "");
    }

    public void translate(String language, String name, String slug, String description) {
        ProductTranslation existing = translations.get(language);
        if (existing == null) {
            translations.put(language, new ProductTranslation(this, language, name, slug, description));
        } else {
            existing.update(name, slug, description);
        }
    }

    public ProductTranslation translation(String language) {
        return translations.get(language);
    }

    /** 当前语言的译文，没有就回退到英文（PARLER_LANGUAGES 里的 'fallback': 'en'） */
    private ProductTranslation current() {
        ProductTranslation t = translations.get(Languages.current());
        return t != null ? t : translations.get(Languages.DEFAULT);
    }

    /** reverse('shop:product_detail', args=[self.id, self.slug]) */
    public String getAbsoluteUrl() {
        return "/" + id + "/" + getSlug() + "/";
    }

    /** {% if product.image %}{{ product.image.url }}{% else %}{% static "img/no_image.png" %}{% endif %} */
    public String getImageUrl() {
        return image.isEmpty() ? "/static/img/no_image.png" : "/media/" + image;
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getName() {
        ProductTranslation t = current();
        return t == null ? "" : t.getName();
    }

    public String getSlug() {
        ProductTranslation t = current();
        return t == null ? "" : t.getSlug();
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image == null ? "" : image;
    }

    public String getDescription() {
        ProductTranslation t = current();
        return t == null ? "" : t.getDescription();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public OffsetDateTime getUpdated() {
        return updated;
    }

    @Override
    public String toString() {
        return getName();
    }
}
