package com.example.myshop.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 美国邮编：12345 或 12345-6789（≈ django-localflavor 的 USZipCodeField）。
 * 自定义 Bean Validation 约束 = 一个注解 + 一个 ConstraintValidator 实现。
 * message 用花括号引用 messages.properties 里的键，所以错误提示也能翻译。
 */
@Documented
@Constraint(validatedBy = UsZipCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UsZipCode {

    String message() default "{validation.us-zip-code}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
