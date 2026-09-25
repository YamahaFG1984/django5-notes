package com.example.myshop.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myshop.shop.Product;
import com.example.myshop.shop.ProductRepository;

/**
 * 把会话里的“id → 数量”变成带商品对象的购物车行（≈ Cart.__iter__）。
 * 一条 IN 查询取回所有商品；已被删除的商品会被静默跳过。
 */
@Service
@Transactional(readOnly = true)
public class CartService {

    /** 模板里遍历的一行：商品、数量、单价、小计 */
    public record Line(Product product, int quantity, BigDecimal price) {
        public BigDecimal totalPrice() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }

    private final Cart cart;
    private final ProductRepository products;

    public CartService(Cart cart, ProductRepository products) {
        this.cart = cart;
        this.products = products;
    }

    public List<Line> lines() {
        Map<Long, Cart.Item> items = cart.items();
        Map<Long, Product> byId = products.findByIdIn(items.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return items.entrySet().stream()
                .filter(entry -> byId.containsKey(entry.getKey()))
                .map(entry -> new Line(byId.get(entry.getKey()), entry.getValue().quantity(), entry.getValue().unitPrice()))
                .toList();
    }
}
