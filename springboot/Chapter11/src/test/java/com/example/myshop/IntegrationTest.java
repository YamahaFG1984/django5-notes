package com.example.myshop;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.myshop.payment.StripeGateway;

/**
 * 所有集成测试的基类。
 * Spring 会缓存测试用的应用上下文，但前提是“配置完全一样”——包括 @MockitoBean 替换了哪些 Bean。
 * 如果每个测试类各 mock 一点，就会各自启动一个上下文（以及一套容器），又慢又占资源。
 * 把会用到的 mock 统一放在基类里，所有测试类共享同一个上下文。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    /** 不真的发邮件 */
    @MockitoBean
    protected JavaMailSender mailSender;

    /** 不真的调用 Stripe */
    @MockitoBean
    protected StripeGateway gateway;
}
