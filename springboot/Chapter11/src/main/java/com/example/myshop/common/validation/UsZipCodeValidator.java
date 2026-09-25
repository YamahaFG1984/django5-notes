package com.example.myshop.common.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UsZipCodeValidator implements ConstraintValidator<UsZipCode, String> {

    private static final Pattern ZIP = Pattern.compile("^\\d{5}(-\\d{4})?$");

    /** 空值交给 @NotBlank 判断，这里只管格式（Bean Validation 的惯例） */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || ZIP.matcher(value.strip()).matches();
    }
}
