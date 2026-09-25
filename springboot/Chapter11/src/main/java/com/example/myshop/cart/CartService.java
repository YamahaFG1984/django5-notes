package com.example.myshop.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myshop.common.Money;
import com.example.myshop.coupons.Coupon;
import com.example.myshop.coupons.CouponRepository;
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

    /** 购物车金额：小计、优惠券、折扣、折后总价（≈ get_total_price / get_discount / get_total_price_after_discount） */
    public record Totals(BigDecimal subtotal, Coupon coupon, BigDecimal discount) {
        public BigDecimal total() {
            return subtotal.subtract(discount);
        }
    }

    private final Cart cart;
    private final ProductRepository products;
    private final CouponRepository coupons;

    public CartService(Cart cart, ProductRepository products, CouponRepository coupons) {
        this.cart = cart;
        this.products = products;
        this.coupons = coupons;
    }

    /** cart.coupon：会话里只存 id，用的时候再查（优惠券被删了就当没有） */
    public Coupon coupon() {
        return cart.getCouponId() == null ? null : coupons.findById(cart.getCouponId()).orElse(null);
    }

    public Totals totals() {
        BigDecimal subtotal = cart.totalPrice();
        Coupon coupon = coupon();
        BigDecimal discount = coupon == null ? BigDecimal.ZERO : Money.percentOf(subtotal, coupon.getDiscount());
        return new Totals(subtotal, coupon, discount);
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
