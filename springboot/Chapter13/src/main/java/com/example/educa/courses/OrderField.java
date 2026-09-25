package com.example.educa.courses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动编号的排序字段，≈ 书中自定义的 {@code OrderField(for_fields=['course'])}：
 * 保存新对象时如果没有指定顺序，就取“同一 scope 下最大值 + 1”（没有则为 0）。
 * 被标注的字段必须是 Integer；实体还要声明 {@code @EntityListeners(OrderFieldListener.class)}。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OrderField {

    /** 在哪个字段相同的范围内编号，比如模块按 "course"、内容按 "module" */
    String scope();
}
