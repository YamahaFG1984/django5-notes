package com.example.myshop.payment;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.myshop.config.MyshopProperties;
import com.example.myshop.config.RabbitConfig;
import com.example.myshop.orders.InvoicePdfRenderer;
import com.example.myshop.orders.Order;
import com.example.myshop.orders.OrderRepository;

/**
 * payment/tasks.py 的 payment_completed：生成 PDF 发票，作为附件发给顾客。
 * 带附件的邮件要用 JavaMailSender + MimeMessageHelper（≈ EmailMessage + email.attach）。
 */
@Component
public class InvoiceWorker {

    private final OrderRepository orders;
    private final InvoicePdfRenderer invoices;
    private final JavaMailSender mailSender;
    private final MyshopProperties properties;

    public InvoiceWorker(OrderRepository orders, InvoicePdfRenderer invoices, JavaMailSender mailSender,
                         MyshopProperties properties) {
        this.orders = orders;
        this.invoices = invoices;
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_COMPLETED_QUEUE)
    @Transactional(readOnly = true)
    public void paymentCompleted(PaymentCompletedMessage message) throws MessagingException {
        Order order = orders.findWithItemsById(message.orderId()).orElse(null);
        if (order == null) {
            return;
        }
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper mail = new MimeMessageHelper(mime, true);   // true = multipart，才能加附件
        mail.setFrom(properties.defaultFromEmail());
        mail.setTo(order.getEmail());
        mail.setSubject("My Shop - Invoice no. " + order.getId());
        mail.setText("Please, find attached the invoice for your recent purchase.");
        mail.addAttachment("order_" + order.getId() + ".pdf", new ByteArrayResource(invoices.render(order)), "application/pdf");
        mailSender.send(mime);
    }
}
