package com.example.myshop.cart;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 上下文处理器 cart/context_processors.py 的 Spring 版：
 * {@code @ControllerAdvice} 里的 {@code @ModelAttribute} 方法会在每个控制器方法之前执行，
 * 返回值放进模型 —— 所有页面的模板都能用 ${cartSummary}。
 * 这里只放数量和总价（base.html 顶部要用），不必每个页面都查询商品表。
 */
@ControllerAdvice
public class CartAdvice {

    public record CartSummary(int totalItems, BigDecimal totalPrice) {
    }

    private final Cart cart;

    public CartAdvice(Cart cart) {
        this.cart = cart;
    }

    @ModelAttribute("cartSummary")
    public CartSummary cartSummary() {
        return new CartSummary(cart.size(), cart.totalPrice());
    }
}
