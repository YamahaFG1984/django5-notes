package com.example.myshop.payment;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myshop.orders.OrderRepository;

@Service
public class PaymentService {

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    public PaymentService(OrderRepository orders, ApplicationEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    /**
     * order.paid = True; order.stripe_id = session.payment_intent; order.save(); payment_completed.delay(order.id)
     * 同一个事件 Stripe 可能重复投递，已经是“已支付”的订单就不再重复发发票。
     */
    @Transactional
    public boolean markPaid(Long orderId, String paymentIntentId) {
        return orders.findById(orderId).map(order -> {
            if (!order.isPaid()) {
                order.setPaid(true);
                order.setStripeId(paymentIntentId);
                events.publishEvent(new PaymentCompletedEvent(order.getId()));
            }
            return true;
        }).orElse(false);
    }
}
