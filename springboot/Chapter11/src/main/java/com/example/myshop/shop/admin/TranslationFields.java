package com.example.myshop.shop.admin;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 后台表单里某一种语言的可翻译字段（≈ parler TranslatableAdmin 里每个语言标签页上的字段）。
 * 是否必填由控制器决定：默认语言必填，其他语言可以留空（留空就回退到默认语言）。
 */
public class TranslationFields {

    @Size(max = 200)
    private String name = "";

    @Size(max = 200)
    @Pattern(regexp = "[-a-zA-Z0-9_]*", message = "只能包含字母、数字、下划线或连字符")
    private String slug = "";

    private String description = "";

    public TranslationFields() {
    }

    public TranslationFields(String name, String slug, String description) {
        this.name = name;
        this.slug = slug;
        this.description = description == null ? "" : description;
    }

    public boolean isBlank() {
        return name.isBlank() && slug.isBlank();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug == null ? "" : slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }
}
