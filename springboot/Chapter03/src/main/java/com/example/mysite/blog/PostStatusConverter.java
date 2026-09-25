package com.example.mysite.blog;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 让 JPA 把 PostStatus 存成 'DF' / 'PB'。
 * 如果直接用 @Enumerated(EnumType.STRING)，存的会是 'DRAFT' / 'PUBLISHED'。
 */
@Converter(autoApply = true)
public class PostStatusConverter implements AttributeConverter<PostStatus, String> {

    @Override
    public String convertToDatabaseColumn(PostStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public PostStatus convertToEntityAttribute(String code) {
        return code == null ? null : PostStatus.fromCode(code);
    }
}
