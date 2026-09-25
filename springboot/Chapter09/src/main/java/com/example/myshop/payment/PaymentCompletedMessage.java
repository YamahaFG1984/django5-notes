package com.example.myshop.payment;

/** ≈ payment_completed.delay(order.id) 的参数 */
public record PaymentCompletedMessage(Long orderId) {
}
