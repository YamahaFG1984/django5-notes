package com.example.myshop.shop;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.myshop.orders.OrderRepository;
import com.example.myshop.payment.PaymentCompletedEvent;

/**
 * 订单支付成功后记录“一起购买”的商品（书中写在 webhook 视图里：r.products_bought(products)）。
 * Redis 不参与数据库事务，所以等支付状态真正提交后再写 Redis。
 * AFTER_COMMIT 阶段原事务已经结束，要查库就得开一个新事务（REQUIRES_NEW）。
 */
@Component
public class RecommendationListener {

    private final OrderRepository orders;
    private final Recommender recommender;

    public RecommendationListener(OrderRepository orders, Recommender recommender) {
        this.orders = orders;
        this.recommender = recommender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        orders.findWithItemsById(event.orderId()).ifPresent(order ->
                recommender.productsBought(order.getItems().stream().map(item -> item.getProduct().getId()).distinct().toList()));
    }
}
