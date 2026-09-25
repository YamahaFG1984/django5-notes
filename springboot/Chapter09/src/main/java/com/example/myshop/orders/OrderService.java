package com.example.myshop.orders;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myshop.cart.Cart;
import com.example.myshop.cart.CartService;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final CartService cartService;
    private final Cart cart;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orders, CartService cartService, Cart cart, ApplicationEventPublisher events) {
        this.orders = orders;
        this.cartService = cartService;
        this.cart = cart;
        this.events = events;
    }

    /**
     * order = form.save() → 逐个创建 OrderItem → cart.clear() → order_created.delay(order.id)
     * 整个方法在一个事务里：订单和订单项要么都保存，要么都不保存。
     */
    @Transactional
    public Order create(OrderCreateForm form) {
        Order order = form.toOrder();
        for (CartService.Line line : cartService.lines()) {
            order.addItem(new OrderItem(line.product(), line.price(), line.quantity()));
        }
        orders.save(order);   // cascade = ALL，订单项随订单一起插入
        cart.clear();
        // 不在这里直接发 MQ 消息：事务还没提交，消费者可能查不到这个订单。见 OrderTasks。
        events.publishEvent(new OrderCreatedEvent(order.getId()));
        return order;
    }
}
