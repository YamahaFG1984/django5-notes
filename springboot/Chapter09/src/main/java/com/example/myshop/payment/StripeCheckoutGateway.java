package com.example.myshop.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.myshop.orders.Order;
import com.example.myshop.orders.OrderItem;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

/** 调用 Stripe API 创建 Checkout Session（≈ stripe.checkout.Session.create(**session_data)）。 */
@Component
public class StripeCheckoutGateway implements StripeGateway {

    private final StripeClient stripe;

    public StripeCheckoutGateway(StripeClient stripe) {
        this.stripe = stripe;
    }

    @Override
    public String createCheckoutSession(Order order, String successUrl, String cancelUrl) {
        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(order.getId().toString())   // webhook 里靠它找回订单
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl);
        for (OrderItem item : order.getItems()) {
            params.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity((long) item.getQuantity())
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd")
                            // Stripe 以“分”为单位：int(item.price * Decimal('100'))
                            .setUnitAmount(item.getPrice().multiply(BigDecimal.valueOf(100)).longValueExact())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(item.getProduct().getName())
                                    .build())
                            .build())
                    .build());
        }
        try {
            Session session = stripe.v1().checkout().sessions().create(params.build());
            return session.getUrl();
        } catch (StripeException e) {
            throw new PaymentException("Could not create Stripe checkout session: " + e.getMessage(), e);
        }
    }

    public static class PaymentException extends RuntimeException {
        public PaymentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
