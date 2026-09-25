package com.example.myshop.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;

import com.example.myshop.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.example.myshop.shop.ProductRepository;

/**
 * 端到端测试异步任务：下单 → 事务提交 → 消息进 RabbitMQ → 监听器消费 → 发邮件。
 * 注意这个测试类<b>没有</b> @Transactional：测试事务不会提交，AFTER_COMMIT 的监听器就永远不会执行。
 */
class OrderNotificationTests extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ProductRepository products;

    @Autowired
    OrderRepository orders;


    @AfterEach
    void cleanUp() {
        orders.deleteAll();
    }

    @Test
    void placingAnOrderSendsConfirmationEmailAsynchronously() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Long productId = products.findAll().getFirst().getId();
        mvc.perform(post("/cart/add/{id}/", productId).session(session).with(csrf()).param("quantity", "1"));
        mvc.perform(post("/orders/create/").session(session).with(csrf())
                .param("firstName", "Ada").param("lastName", "Lovelace").param("email", "ada@example.com")
                .param("address", "1 Main St").param("postalCode", "12345").param("city", "London"));

        Long orderId = orders.findAll().getFirst().getId();
        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> verify(mailSender, atLeastOnce()).send(any(SimpleMailMessage.class)));
        verify(mailSender, atLeastOnce()).send(mail.capture());
        assertThat(mail.getValue().getSubject()).isEqualTo("Order nr. " + orderId);
        assertThat(mail.getValue().getTo()).containsExactly("ada@example.com");
        assertThat(mail.getValue().getText()).startsWith("Dear Ada,");
    }
}
