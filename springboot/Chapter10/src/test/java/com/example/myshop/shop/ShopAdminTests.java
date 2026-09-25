package com.example.myshop.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import com.example.myshop.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.example.myshop.account.CurrentUser;

@Transactional
class ShopAdminTests extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ProductRepository products;

    @Autowired
    CategoryRepository categories;


    private final RequestPostProcessor staff = user(new CurrentUser(1L, "admin", "x", true,
            List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_STAFF"))));

    @Test
    void adminRequiresStaff() throws Exception {
        mvc.perform(get("/admin/shop/product/")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/shop/product/").with(staff)).andExpect(status().isOk());
    }

    @Test
    void listEditableSavesPriceAndAvailabilityOfManyRows() throws Exception {
        List<Product> all = products.findAllByOrderByNameAsc();   // Espresso, Green tea, Red tea, Tea powder
        var request = post("/admin/shop/product/").with(staff).with(csrf());
        for (int i = 0; i < all.size(); i++) {
            Product p = all.get(i);
            request.param("rows[" + i + "].id", p.getId().toString())
                    .param("rows[" + i + "].price", i == 0 ? "20.00" : p.getPrice().toPlainString());
            // 复选框：勾选的才会提交 true；Thymeleaf 还会生成隐藏的 _available 字段表示“未勾选=false”
            request.param("_rows[" + i + "].available", "on");
            if (i != 1) {
                request.param("rows[" + i + "].available", "true");
            }
        }
        mvc.perform(request).andExpect(redirectedUrl("/admin/shop/product/"));

        assertThat(products.findById(all.get(0).getId()).orElseThrow().getPrice()).isEqualByComparingTo("20.00");
        assertThat(products.findById(all.get(1).getId()).orElseThrow().isAvailable()).isFalse();
        assertThat(products.findById(all.get(2).getId()).orElseThrow().isAvailable()).isTrue();
    }

    @Test
    void addProductWithImage() throws Exception {
        Long teaId = categories.findBySlug("tea").orElseThrow().getId();
        mvc.perform(multipart("/admin/shop/product/add/").file(new MockMultipartFile("image", "oolong.png", "image/png", png()))
                        .param("categoryId", teaId.toString()).param("name", "Oolong").param("slug", "oolong")
                        .param("price", "12.34").param("available", "true")
                        .with(staff).with(csrf()))
                .andExpect(redirectedUrl("/admin/shop/product/"));
        Product oolong = products.findAll().stream().filter(p -> p.getSlug().equals("oolong")).findFirst().orElseThrow();
        assertThat(oolong.getImageUrl()).startsWith("/media/products/");
    }

    private static byte[] png() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
