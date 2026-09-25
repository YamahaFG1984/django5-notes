package com.example.myshop.shop.admin;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.multipart.MultipartFile;

import com.example.myshop.i18n.Languages;
import com.example.myshop.shop.Product;
import com.example.myshop.shop.ProductTranslation;

/**
 * 后台“编辑商品”表单。可翻译字段按语言放在 map 里，
 * 请求参数形如 translations[es].name=Té verde —— Spring 的数据绑定支持 Map 的键。
 */
public class ProductForm {

    @NotNull
    private Long categoryId;

    @Valid
    private Map<String, TranslationFields> translations = emptyTranslations();

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    private boolean available = true;

    private MultipartFile image;

    static Map<String, TranslationFields> emptyTranslations() {
        Map<String, TranslationFields> map = new LinkedHashMap<>();
        Languages.ALL.forEach(l -> map.put(l.code(), new TranslationFields()));
        return map;
    }

    public static ProductForm of(Product product) {
        ProductForm form = new ProductForm();
        form.categoryId = product.getCategory().getId();
        form.price = product.getPrice();
        form.available = product.isAvailable();
        Languages.ALL.forEach(l -> {
            ProductTranslation t = product.translation(l.code());
            if (t != null) {
                form.translations.put(l.code(), new TranslationFields(t.getName(), t.getSlug(), t.getDescription()));
            }
        });
        return form;
    }

    public TranslationFields translation(String language) {
        return translations.computeIfAbsent(language, k -> new TranslationFields());
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

    public Map<String, TranslationFields> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, TranslationFields> translations) {
        this.translations = translations;
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
