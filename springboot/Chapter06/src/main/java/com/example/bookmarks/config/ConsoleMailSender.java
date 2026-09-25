package com.example.bookmarks.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

/**
 * 开发用“邮件后端”：不真正发送，只把邮件打印到日志。
 * 等价于 Django 的 EMAIL_BACKEND = 'django.core.mail.backends.console.EmailBackend'。
 * 以 smtp profile 启动时（--spring.profiles.active=smtp）这个 Bean 不会创建，
 * Spring Boot 会根据 spring.mail.* 自动配置真正的 JavaMailSender。
 */
@Component
@Profile("!smtp")
public class ConsoleMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMailSender.class);

    @Override
    public void send(SimpleMailMessage... messages) {
        for (SimpleMailMessage message : messages) {
            log.info("""
                    
                    ---------- 邮件（console 后端，未真正发送）----------
                    From: {}
                    To: {}
                    Subject: {}
                    
                    {}
                    -----------------------------------------------------""",
                    message.getFrom(), String.join(", ", message.getTo()), message.getSubject(), message.getText());
        }
    }
}
