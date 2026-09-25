package com.example.myshop.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 金额工具：统一保留两位小数、四舍五入（书中用模板过滤器 floatformat:2 只在显示时处理）。 */
public final class Money {

    private Money() {
    }

    /** amount × percent%，比如 percentOf(135.50, 10) = 13.55 */
    public static BigDecimal percentOf(BigDecimal amount, int percent) {
        return amount.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
