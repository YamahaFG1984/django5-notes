package com.example.mysite.blog;

import java.util.Arrays;

/**
 * 对应 Django 的 {@code class Status(models.TextChoices)}：
 * 数据库里存两个字母的代码（DF / PB），界面上显示可读标签。
 */
public enum PostStatus {

    DRAFT("DF", "Draft"),
    PUBLISHED("PB", "Published");

    private final String code;
    private final String label;

    PostStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static PostStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知状态代码: " + code));
    }
}
