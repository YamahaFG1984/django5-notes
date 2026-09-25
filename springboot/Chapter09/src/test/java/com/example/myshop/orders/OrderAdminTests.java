package com.example.myshop.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import java.math.BigDecimal;
import java.util.List;

import com.example.myshop.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.example.myshop.account.CurrentUser;
import com.example.myshop.shop.ProductRepository;

@Transactional
class OrderAdminTests extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    OrderRepository orders;

    @Autowired
    ProductRepository products;

    @Autowired
    InvoicePdfRenderer invoices;


    private final RequestPostProcessor staff = user(new CurrentUser(1L, "admin", "x", true,
            List.of(new SimpleGrantedAuthority("ROLE_STAFF"))));

    Order first;
    Order second;

    @BeforeEach
    void setUp() {
        first = order("Ada", "30.00");
        second = order("Grace", "12.50");
        second.setPaid(true);
        second.setStripeId("pi_123");
    }

    private Order order(String name, String price) {
        Order order = new Order(name, "Smith", name.toLowerCase() + "@example.com", "1 Main St", "12345", "London");
        order.addItem(new OrderItem(products.findAll().getFirst(), new BigDecimal(price), 1));
        return orders.save(order);
    }

    @Test
    void listFiltersByPaid() throws Exception {
        mvc.perform(get("/admin/orders/order/").param("paid", "true").with(staff))
                .andExpect(content().string(containsString("grace@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("ada@example.com"))))
                .andExpect(content().string(containsString("https://dashboard.stripe.com/test/payments/pi_123")));
    }

    @Test
    void exportSelectedOrdersToCsv() throws Exception {
        String csv = mvc.perform(post("/admin/orders/order/action/").with(staff).with(csrf())
                        .param("action", "export_to_csv")
                        .param("ids", first.getId().toString(), second.getId().toString()))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"order.csv\""))
                .andReturn().getResponse().getContentAsString();
        List<String> lines = csv.lines().toList();
        assertThat(lines).hasSize(3);
        assertThat(lines.getFirst()).startsWith("ID,first name,last name,email");
        assertThat(lines.get(2)).contains("Grace", "true", "pi_123");

        mvc.perform(post("/admin/orders/order/action/").with(staff).with(csrf()).param("action", "export_to_csv"))
                .andExpect(redirectedUrl("/admin/orders/order/"));
    }

    @Test
    void detailAndPdfInvoice() throws Exception {
        mvc.perform(get("/admin/orders/order/{id}/detail/", first.getId()).with(staff))
                .andExpect(content().string(containsString("Pending payment")))
                .andExpect(content().string(containsString("$30.00")));
        byte[] pdf = mvc.perform(get("/admin/orders/order/{id}/pdf/", first.getId()).with(staff))
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(1000);
    }
}
