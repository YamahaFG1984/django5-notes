package com.example.educa.common;

import java.io.InputStream;
import java.util.Arrays;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 没有配置 SMTP 服务器（spring.mail.host 为空）时，把邮件打印到日志里，
 * ≈ EMAIL_BACKEND = 'django.core.mail.backends.console.EmailBackend'。
 */
@Component
@ConditionalOnExpression("'${spring.mail.host:}' == ''")
public class ConsoleMailSender implements JavaMailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMailSender.class);

    @Override
    public void send(SimpleMailMessage... messages) throws MailException {
        for (SimpleMailMessage m : messages) {
            log.info("""

                    From: {}
                    To: {}
                    Subject: {}

                    {}
                    """, m.getFrom(), Arrays.toString(m.getTo()), m.getSubject(), m.getText());
        }
    }

    @Override
    public MimeMessage createMimeMessage() {
        return new MimeMessage((Session) null);
    }

    @Override
    public MimeMessage createMimeMessage(InputStream contentStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(MimeMessage... mimeMessages) throws MailException {
        log.info("{} MIME message(s) not sent (console mail sender)", mimeMessages.length);
    }
}
