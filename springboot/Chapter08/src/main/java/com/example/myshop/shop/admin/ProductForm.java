package com.example.myshop.shop.admin;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.web.multipart.MultipartFile;

import com.example.myshop.shop.Product;

/** 后台“编辑商品”表单（Django admin 根据 Product 模型自动生成的那个表单）。 */
public class ProductForm {

    @NotNull
    private Long categoryId;

    @NotBlank @Size(max = 200)
    private String name;

    @NotBlank @Size(max = 200)
    @Pattern(regexp = "[-a-zA-Z0-9_]+", message = "只能包含字母、数字、下划线或连字符")
    private String slug;

    private String description = "";

    /** DecimalField(max_digits=10, decimal_places=2) */
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    private boolean available = true;

    private MultipartFile image;

    public static ProductForm of(Product product) {
        ProductForm form = new ProductForm();
        form.categoryId = product.getCategory().getId();
        form.name = product.getName();
        form.slug = product.getSlug();
        form.description = product.getDescription();
        form.price = product.getPrice();
        form.available = product.isAvailable();
        return form;
    }

    public boolean hasNewImage() {
        return image != null && !image.isEmpty();
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }
}
