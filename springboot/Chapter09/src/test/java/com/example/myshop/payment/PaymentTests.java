package com.example.myshop.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import com.example.myshop.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.myshop.config.StripeProperties;
import com.example.myshop.orders.Order;
import com.example.myshop.orders.OrderController;
import com.example.myshop.orders.OrderItem;
import com.example.myshop.orders.OrderRepository;
import com.example.myshop.shop.ProductRepository;

@Transactional
class PaymentTests extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    OrderRepository orders;

    @Autowired
    ProductRepository products;

    @Autowired
    StripeProperties stripe;



    Order order;
    MockHttpSession session;

    @BeforeEach
    void setUp() {
        order = new Order("Ada", "Lovelace", "ada@example.com", "1 Main St", "12345", "London");
        order.addItem(new OrderItem(products.findAll().getFirst(), new BigDecimal("30.00"), 2));
        orders.save(order);
        session = new MockHttpSession();
        session.setAttribute(OrderController.ORDER_ID_SESSION_KEY, order.getId());
    }

    @Test
    void processShowsSummaryAndRedirectsToStripeWith303() throws Exception {
        mvc.perform(get("/payment/process/")).andExpect(status().isNotFound());   // 会话里没有订单
        mvc.perform(get("/payment/process/").session(session))
                .andExpect(content().string(containsString("$60.00")));

        when(gateway.createCheckoutSession(any(), eq("http://localhost/payment/completed/"), eq("http://localhost/payment/canceled/")))
                .thenReturn("https://checkout.stripe.com/c/pay/cs_test_1");
        mvc.perform(post("/payment/process/").session(session).with(csrf()))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "https://checkout.stripe.com/c/pay/cs_test_1"));
        verify(gateway).createCheckoutSession(any(Order.class), any(), any());
    }

    @Test
    void webhookRejectsBadSignatures() throws Exception {
        String payload = StripeSignatures.checkoutCompleted(order.getId(), "pi_test_1");
        mvc.perform(post("/payment/webhook/").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/payment/webhook/").contentType(MediaType.APPLICATION_JSON).content(payload)
                        .header("Stripe-Signature", StripeSignatures.sign(payload, "whsec_wrong")))
                .andExpect(status().isBadRequest());
        assertThat(orders.findById(order.getId()).orElseThrow().isPaid()).isFalse();
    }

    @Test
    void validWebhookMarksOrderAsPaid() throws Exception {
        String payload = StripeSignatures.checkoutCompleted(order.getId(), "pi_test_1");
        // 注意没有 .with(csrf())：webhook 在 SecurityConfig 里豁免了 CSRF
        mvc.perform(post("/payment/webhook/").contentType(MediaType.APPLICATION_JSON).content(payload)
                        .header("Stripe-Signature", StripeSignatures.sign(payload, stripe.webhookSecret())))
                .andExpect(status().isOk());
        Order paid = orders.findById(order.getId()).orElseThrow();
        assertThat(paid.isPaid()).isTrue();
        assertThat(paid.getStripeId()).isEqualTo("pi_test_1");
        assertThat(paid.stripeUrl(true)).isEqualTo("https://dashboard.stripe.com/test/payments/pi_test_1");

        String unknown = StripeSignatures.checkoutCompleted(999L, "pi_test_2");
        mvc.perform(post("/payment/webhook/").contentType(MediaType.APPLICATION_JSON).content(unknown)
                        .header("Stripe-Signature", StripeSignatures.sign(unknown, stripe.webhookSecret())))
                .andExpect(status().isNotFound());
    }
}
