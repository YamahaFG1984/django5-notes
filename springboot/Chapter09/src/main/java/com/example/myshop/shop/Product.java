package com.example.myshop.shop;

import java.math.BigDecimal;
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
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 商品。价格用 BigDecimal —— 对应 DecimalField(max_digits=10, decimal_places=2)。
 * 永远不要用 double 表示钱：0.1 + 0.2 != 0.3。
 */
@Entity
@Table(name = "shop_product", indexes = {
        @Index(name = "shop_product_id_slug_idx", columnList = "id, slug"),
        @Index(name = "shop_product_name_idx", columnList = "name"),
        @Index(name = "shop_product_created_idx", columnList = "created DESC")})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** related_name='products' */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String slug;

    /** ImageField(upload_to='products/%Y/%m/%d', blank=True)：空字符串表示没有图片 */
    @Column(nullable = false, length = 100)
    private String image = "";

    @Column(nullable = false, columnDefinition = "text")
    private String description = "";

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

    protected Product() {
    }

    public Product(Category category, String name, String slug, BigDecimal price) {
        this.category = category;
        this.name = name;
        this.slug = slug;
        this.price = price;
    }

    /** reverse('shop:product_detail', args=[self.id, self.slug]) */
    public String getAbsoluteUrl() {
        return "/" + id + "/" + slug + "/";
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
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image == null ? "" : image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
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
        return name;
    }
}
