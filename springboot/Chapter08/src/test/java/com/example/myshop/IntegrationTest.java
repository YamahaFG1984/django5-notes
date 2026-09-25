package com.example.myshop;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


/**
 * 所有集成测试的基类：统一注解和 mock，让所有测试类共享同一个 Spring 上下文
 * （上下文缓存的键包括 @MockitoBean 替换了哪些 Bean）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    /** 不真的发邮件 */
    @MockitoBean
    protected MailSender mailSender;
}
