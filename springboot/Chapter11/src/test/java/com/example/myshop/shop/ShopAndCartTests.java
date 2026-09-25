package com.example.myshop.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import com.example.myshop.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.myshop.cart.CartService;
import com.example.myshop.orders.Order;
import com.example.myshop.orders.OrderRepository;

/** 测试库里已经由 db/sample 迁移放了 4 个示例商品（2 个分类）。 */
@Transactional
class ShopAndCartTests extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ProductRepository products;

    @Autowired
    CategoryRepository categories;

    @Autowired
    OrderRepository orders;


    /** 同一个会话里连续发请求，购物车才能“记住”东西（≈ Django 测试客户端自动保存 cookie） */
    MockHttpSession session;
    Product greenTea;
    Product redTea;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        greenTea = products.findAll().stream().filter(p -> p.getSlug().equals("green-tea")).findFirst().orElseThrow();
        redTea = products.findAll().stream().filter(p -> p.getSlug().equals("red-tea")).findFirst().orElseThrow();
    }

    @Test
    void catalogListsAvailableProductsByCategory() throws Exception {
        greenTea.setAvailable(false);
        mvc.perform(get("/en/"))
                .andExpect(content().string(containsString("Red tea")))
                .andExpect(content().string(not(containsString("Green tea"))));
        mvc.perform(get("/en/coffee/"))
                .andExpect(content().string(containsString("Espresso")))
                .andExpect(content().string(not(containsString("Red tea"))));
        mvc.perform(get("/en/no-such-category/")).andExpect(status().isNotFound());
        mvc.perform(get("/en" + greenTea.getAbsoluteUrl())).andExpect(status().isNotFound());
        mvc.perform(get("/en" + redTea.getAbsoluteUrl()))
                .andExpect(content().string(containsString("$45.50")))
                .andExpect(content().string(containsString("/static/img/no_image.png")));
    }

    @Test
    void cartAddUpdateRemove() throws Exception {
        mvc.perform(post("/en/cart/add/{id}/", greenTea.getId()).session(session).with(csrf())
                        .param("quantity", "2").param("override", "false"))
                .andExpect(redirectedUrl("/en/cart/"));
        mvc.perform(post("/en/cart/add/{id}/", greenTea.getId()).session(session).with(csrf())
                        .param("quantity", "1").param("override", "false"));
        mvc.perform(post("/en/cart/add/{id}/", redTea.getId()).session(session).with(csrf())
                        .param("quantity", "1").param("override", "false"));

        mvc.perform(get("/en/cart/").session(session))
                .andExpect(content().string(containsString("4 items,")))
                .andExpect(content().string(containsString("$135.50")));   // 3 × 30.00 + 45.50

        // override=true：数量设为 1，而不是再加 1
        mvc.perform(post("/en/cart/add/{id}/", greenTea.getId()).session(session).with(csrf())
                .param("quantity", "1").param("override", "true"));
        mvc.perform(post("/en/cart/remove/{id}/", redTea.getId()).session(session).with(csrf()));
        mvc.perform(get("/en/").session(session))
                .andExpect(content().string(containsString("1 item,")))
                .andExpect(content().string(containsString("$30.00")));

        // 另一个会话看不到这个购物车
        mvc.perform(get("/en/")).andExpect(content().string(containsString("Your cart is empty.")));
    }

    @Test
    void invalidQuantityIsIgnored() throws Exception {
        mvc.perform(post("/en/cart/add/{id}/", greenTea.getId()).session(session).with(csrf()).param("quantity", "99"));
        mvc.perform(get("/en/cart/").session(session))
                .andExpect(model().attribute("lines", List.of()));
    }

    @Test
    void checkoutCreatesOrderWithItemsAndClearsCart() throws Exception {
        mvc.perform(get("/en/orders/create/").session(session)).andExpect(redirectedUrl("/en/cart/"));

        mvc.perform(post("/en/cart/add/{id}/", redTea.getId()).session(session).with(csrf()).param("quantity", "2"));
        mvc.perform(post("/en/orders/create/").session(session).with(csrf()).param("firstName", ""))
                .andExpect(model().attributeHasFieldErrors("form", "firstName", "email", "city"));

        // 下单后商品调价，不影响订单里记录的价格
        mvc.perform(post("/en/orders/create/").session(session).with(csrf())
                        .param("firstName", "Ada").param("lastName", "Lovelace").param("email", "ada@example.com")
                        .param("address", "1 Main St").param("postalCode", "12345").param("city", "London"))
                .andExpect(redirectedUrl("/en/payment/process/"));
        redTea.setPrice(new BigDecimal("99.00"));

        Order order = orders.findAll().getFirst();
        Order withItems = orders.findWithItemsById(order.getId()).orElseThrow();
        assertThat(withItems.getItems()).hasSize(1);
        assertThat(withItems.getTotalCost()).isEqualByComparingTo("91.00");
        mvc.perform(get("/en/").session(session)).andExpect(content().string(containsString("Your cart is empty.")));
    }
}
