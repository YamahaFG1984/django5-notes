package com.example.myshop.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Duration;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import com.example.myshop.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.myshop.config.StripeProperties;
import com.example.myshop.orders.Order;
import com.example.myshop.orders.OrderItem;
import com.example.myshop.orders.OrderRepository;
import com.example.myshop.shop.ProductRepository;

/** webhook → 事务提交 → RabbitMQ → InvoiceWorker → 带 PDF 附件的邮件。不能加 @Transactional（见第 8 章）。 */
class InvoiceEmailTests extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    OrderRepository orders;

    @Autowired
    ProductRepository products;

    @Autowired
    StripeProperties stripe;


    @Autowired
    com.example.myshop.shop.Recommender recommender;


    @AfterEach
    void cleanUp() {
        orders.deleteAll();
    }

    @Autowired
    org.springframework.data.redis.core.StringRedisTemplate redis;

    @Test
    void paidOrderReceivesPdfInvoice() throws Exception {
        redis.getConnectionFactory().getConnection().serverCommands().flushDb();   // Redis 不随测试回滚，先清空
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        Order order = new Order("Ada", "Lovelace", "ada@example.com", "1 Main St", "12345", "London");
        var first = products.findAll().get(0);
        var second = products.findAll().get(1);
        order.addItem(new OrderItem(first, new BigDecimal("30.00"), 1));
        order.addItem(new OrderItem(second, new BigDecimal("45.50"), 1));
        orders.save(order);

        String payload = StripeSignatures.checkoutCompleted(order.getId(), "pi_test_9");
        mvc.perform(post("/payment/webhook/").contentType(MediaType.APPLICATION_JSON).content(payload)
                        .header("Stripe-Signature", StripeSignatures.sign(payload, stripe.webhookSecret())))
                .andExpect(status().isOk());

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> verify(mailSender, atLeastOnce()).send(sent.capture()));
        MimeMessage mail = sent.getValue();
        assertThat(mail.getSubject()).isEqualTo("My Shop - Invoice no. " + order.getId());
        MimeMultipart multipart = (MimeMultipart) mail.getContent();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mail.writeTo(out);
        assertThat(out.toString()).contains("filename=order_" + order.getId() + ".pdf");
        assertThat(multipart.getCount()).isGreaterThanOrEqualTo(2);

        // 支付成功后，一起购买的商品被记进 Redis（RecommendationListener）
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(recommender.suggestProductsFor(java.util.List.of(first.getId()), 4))
                        .extracting(com.example.myshop.shop.Product::getId).containsExactly(second.getId()));
    }
}
