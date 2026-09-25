package com.example.myshop.coupons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.myshop.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.myshop.account.CurrentUser;
import com.example.myshop.orders.Order;
import com.example.myshop.orders.OrderRepository;
import com.example.myshop.shop.ProductRepository;

@Transactional
class CouponTests extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CouponRepository coupons;

    @Autowired
    ProductRepository products;

    @Autowired
    OrderRepository orders;


    MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        coupons.save(new Coupon("SUMMER", now.minusDays(1), now.plusDays(1), 10, true));
        coupons.save(new Coupon("EXPIRED", now.minusDays(10), now.minusDays(1), 50, true));
        coupons.save(new Coupon("OFF", now.minusDays(1), now.plusDays(1), 50, false));
        session = new MockHttpSession();
        Long redTea = products.findAll().stream().filter(p -> p.getSlug().equals("red-tea")).findFirst().orElseThrow().getId();
        Long greenTea = products.findAll().stream().filter(p -> p.getSlug().equals("green-tea")).findFirst().orElseThrow().getId();
        mvc.perform(post("/en/cart/add/{id}/", greenTea).session(session).with(csrf()).param("quantity", "3"));
        mvc.perform(post("/en/cart/add/{id}/", redTea).session(session).with(csrf()).param("quantity", "1"));
    }

    private void apply(String code) throws Exception {
        mvc.perform(post("/en/coupons/apply/").session(session).with(csrf()).param("code", code))
                .andExpect(redirectedUrl("/en/cart/"));
    }

    @Test
    void validCouponIsCaseInsensitiveAndShownInCart() throws Exception {
        apply("summer");
        mvc.perform(get("/en/cart/").session(session))
                .andExpect(content().string(containsString("&quot;SUMMER&quot; coupon (10% off)")))
                .andExpect(content().string(containsString("- $13.55")))      // 135.50 × 10%
                .andExpect(content().string(containsString("$121.95")));
    }

    @Test
    void expiredOrInactiveCouponsAreRejected() throws Exception {
        apply("SUMMER");
        apply("EXPIRED");   // 无效的码会清除已应用的优惠券（与书中一致）
        mvc.perform(get("/en/cart/").session(session)).andExpect(content().string(not(containsString("coupon ("))));
        apply("OFF");
        mvc.perform(get("/en/cart/").session(session)).andExpect(content().string(not(containsString("coupon ("))));
    }

    @Test
    void orderKeepsCouponAndDiscount() throws Exception {
        apply("SUMMER");
        mvc.perform(post("/en/orders/create/").session(session).with(csrf())
                        .param("firstName", "Ada").param("lastName", "Lovelace").param("email", "ada@example.com")
                        .param("address", "1 Main St").param("postalCode", "12345").param("city", "London"))
                .andExpect(redirectedUrl("/en/payment/process/"));
        Order order = orders.findWithItemsById(orders.findAll().getFirst().getId()).orElseThrow();
        assertThat(order.getCoupon().getCode()).isEqualTo("SUMMER");
        assertThat(order.getDiscount()).isEqualTo(10);
        assertThat(order.getTotalCostBeforeDiscount()).isEqualByComparingTo("135.50");
        assertThat(order.getTotalCost()).isEqualByComparingTo("121.95");

        // 支付页显示折扣明细
        mvc.perform(get("/en/payment/process/").session(session))
                .andExpect(content().string(containsString("- $13.55")));
    }

    @Test
    void adminValidatesCouponRange() throws Exception {
        var staff = user(new CurrentUser(1L, "admin", "x", true, List.of(new SimpleGrantedAuthority("ROLE_STAFF"))));
        mvc.perform(post("/admin/coupons/coupon/add/").with(staff).with(csrf())
                        .param("code", "summer").param("validFrom", "2025-01-02T00:00").param("validTo", "2025-01-01T00:00")
                        .param("discount", "150"))
                .andExpect(model().attributeHasFieldErrorCode("form", "code", "unique"))
                .andExpect(model().attributeHasFieldErrorCode("form", "validTo", "range"))
                .andExpect(model().attributeHasFieldErrors("form", "discount"));
    }
}
