package com.example.myshop.i18n;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.example.myshop.IntegrationTest;

/**
 * 示例数据里：绿茶、红茶、两个分类有西班牙文译文；Tea powder、Espresso 只有英文（用来测试回退）。
 * 故意<b>不加</b> @Transactional：测试事务会让 Hibernate 会话一直开着，掩盖懒加载问题
 * （这个类最初加了 @Transactional，结果真实运行时报 LazyInitializationException，测试却全绿）。
 */
class I18nTests extends IntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    com.example.myshop.orders.OrderRepository orders;

    @org.junit.jupiter.api.AfterEach
    void cleanUp() {
        orders.deleteAll();   // 没有测试事务，自己清理
    }

    @Test
    void spanishCheckoutAndPaymentPages() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/es/carro/add/2/").session(session).with(csrf()).param("quantity", "1"));
        mvc.perform(post("/es/pedidos/crear/").session(session).with(csrf())
                        .param("firstName", "Ana").param("lastName", "García").param("email", "ana@example.com")
                        .param("address", "Calle Mayor 1").param("postalCode", "12345").param("city", "Madrid"))
                .andExpect(redirectedUrl("/es/pago/procesar/"));
        mvc.perform(get("/es/pago/procesar/").session(session))
                .andExpect(content().string(containsString("Resumen de compra")))
                .andExpect(content().string(containsString("Té rojo")))
                .andExpect(content().string(containsString("$45,50")));
    }

    @Test
    void unprefixedUrlsRedirectByAcceptLanguage() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/en/"));
        mvc.perform(get("/").locale(Locale.forLanguageTag("es-ES"))).andExpect(redirectedUrl("/es/"));
        mvc.perform(get("/cart/").locale(Locale.forLanguageTag("es"))).andExpect(redirectedUrl("/es/carro/"));
        mvc.perform(get("/tea/?x=1")).andExpect(redirectedUrl("/en/tea/?x=1"));
    }

    @Test
    void spanishPagesUseTranslatedTextModelsAndFormats() throws Exception {
        mvc.perform(get("/es/"))
                .andExpect(content().string(containsString("class=\"logo\">Mi tienda</a>")))
                .andExpect(content().string(containsString("Categorías")))
                .andExpect(content().string(containsString("Té verde")))
                .andExpect(content().string(containsString("Tea powder")))          // 没有西语译文：回退到英文
                .andExpect(content().string(containsString("$45,50")))              // 西班牙语的小数点是逗号
                .andExpect(content().string(containsString("href=\"/es/te/\"")))   // 分类 slug 也翻译了
                .andExpect(content().string(containsString("href=\"/es/1/te-verde/\"")));
        mvc.perform(get("/en/"))
                .andExpect(content().string(containsString("Green tea")))
                .andExpect(content().string(containsString("$45.50")));
    }

    @Test
    void translatedSlugsResolveOnlyInTheirLanguage() throws Exception {
        mvc.perform(get("/es/te/")).andExpect(content().string(containsString("Té rojo")));
        mvc.perform(get("/es/1/te-verde/")).andExpect(content().string(containsString("Añadir al carro")));
        mvc.perform(get("/en/1/te-verde/")).andExpect(status().isNotFound());
        mvc.perform(get("/en/1/green-tea/")).andExpect(content().string(containsString("Add to cart")));
    }

    @Test
    void translatedUrlSegmentsAndPlurals() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/es/1/te-verde/").session(session))
                .andExpect(content().string(containsString("action=\"/es/carro/add/1/\"")));
        mvc.perform(post("/es/carro/add/1/").session(session).with(csrf()).param("quantity", "1"))
                .andExpect(redirectedUrl("/es/carro/"));
        mvc.perform(get("/es/carro/").session(session))
                .andExpect(content().string(containsString("1 producto, $30,00")))
                .andExpect(content().string(containsString("href=\"/es/pedidos/crear/\"")));
        mvc.perform(post("/es/carro/add/1/").session(session).with(csrf()).param("quantity", "1"));
        mvc.perform(get("/en/cart/").session(session))
                .andExpect(content().string(containsString("2 items, $60.00")));
    }

    @Test
    void validationMessagesAreTranslated() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/es/carro/add/1/").session(session).with(csrf()).param("quantity", "1"));
        mvc.perform(post("/es/pedidos/crear/").session(session).with(csrf())
                        .param("firstName", "").param("postalCode", "ABC"))
                .andExpect(content().string(containsString("Introduzca un código postal")))
                .andExpect(content().string(containsString("no debe estar vacío")))
                .andExpect(content().string(containsString("Código postal:")));
        mvc.perform(post("/en/orders/create/").session(session).with(csrf())
                        .param("firstName", "").param("postalCode", "ABC"))
                .andExpect(content().string(containsString("Enter a zip code in the format XXXXX or XXXXX-XXXX.")))
                .andExpect(content().string(not(containsString("Introduzca"))));
    }
}
