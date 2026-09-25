package com.example.myshop.cart;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.example.myshop.shop.Product;

/**
 * 会话购物车，对应 cart/cart.py 的 Cart。
 * <p>
 * Django 版每次请求都 {@code Cart(request)}，自己从 request.session['cart'] 里取字典。
 * Spring 里用 {@code @SessionScope}：容器为每个 HTTP 会话创建一个 Cart 实例并存进 HttpSession，
 * 注入到控制器里的其实是一个代理，调用时自动找到“当前会话”的那个实例。
 * <p>
 * 会话里只存“商品 id → 数量和当时的价格”，不存商品对象本身（和 Django 一样），
 * 所以要实现 Serializable，会话才能被持久化或在集群间复制。
 */
@Component
@SessionScope
public class Cart implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 加入购物车时的单价，用字符串保存 —— Django 版用 str(product.price) 也是为了能 JSON 序列化 */
    public record Item(int quantity, String price) implements Serializable {
        public BigDecimal unitPrice() {
            return new BigDecimal(price);
        }

        public BigDecimal totalPrice() {
            return unitPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }

    private final Map<Long, Item> items = new LinkedHashMap<>();

    /** 当前使用的优惠券 id（≈ request.session['coupon_id']），null 表示没有 */
    private Long couponId;

    /** cart.add(product, quantity, override_quantity) */
    public void add(Product product, int quantity, boolean overrideQuantity) {
        Item current = items.get(product.getId());
        int newQuantity = overrideQuantity || current == null ? quantity : current.quantity() + quantity;
        String price = current == null ? product.getPrice().toPlainString() : current.price();
        items.put(product.getId(), new Item(newQuantity, price));
    }

    public void remove(Long productId) {
        items.remove(productId);
    }

    /** 下单后清空购物车；书中只删了购物车，优惠券会留到下一次购物，这里一起清掉 */
    public void clear() {
        items.clear();
        couponId = null;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public Map<Long, Item> items() {
        return Map.copyOf(items);
    }

    /** len(cart)：所有商品的件数之和 */
    public int size() {
        return items.values().stream().mapToInt(Item::quantity).sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public BigDecimal totalPrice() {
        return items.values().stream().map(Item::totalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
