package com.example.myshop.common;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 按语言格式化数字（≈ Django 的本地化格式，USE_L10N）：英文 $1,234.50，西班牙文 $1.234,50。
 * 模板里用 ${#fmt.money(product.price)}。
 */
public class Formats {

    private final Locale locale;

    public Formats(Locale locale) {
        this.locale = locale;
    }

    public String money(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return "$" + format.format(amount == null ? BigDecimal.ZERO : amount);
    }
}
