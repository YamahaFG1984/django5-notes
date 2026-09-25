package com.example.myshop.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.myshop.config.StripeProperties;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

/**
 * Stripe 的回调（payment/webhooks.py 的 stripe_webhook）。
 * 必须拿到<b>原始请求体</b>字符串来校验签名 —— 所以参数是 @RequestBody String，而不是自动转换的对象。
 * CSRF 豁免在 SecurityConfig 里配置。
 */
@RestController
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeProperties properties;
    private final PaymentService payments;

    public StripeWebhookController(StripeProperties properties, PaymentService payments) {
        this.properties = properties;
        this.payments = payments;
    }

    @PostMapping("/payment/webhook/")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader(name = "Stripe-Signature", required = false) String signature) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, properties.webhookSecret());
        } catch (SignatureVerificationException | RuntimeException e) {
            // 无效的签名，或请求体不是合法的 JSON（stripe-java 抛出运行时异常）：拒绝
            // ≈ except ValueError / except stripe.error.SignatureVerificationError
            return ResponseEntity.badRequest().build();
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) dataObject(event);
            if ("payment".equals(session.getMode()) && "paid".equals(session.getPaymentStatus())) {
                boolean found = payments.markPaid(Long.valueOf(session.getClientReferenceId()), session.getPaymentIntent());
                if (!found) {
                    return ResponseEntity.notFound().build();
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    /**
     * event.data.object。stripe-java 只在事件的 API 版本与 SDK 一致时才“安全”反序列化，
     * 版本不一致时退回 deserializeUnsafe()（Stripe 控制台里可以把 webhook 端点固定到 SDK 的版本）。
     */
    private static StripeObject dataObject(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        return deserializer.getObject().orElseGet(() -> {
            try {
                log.warn("Webhook 事件的 API 版本 {} 与 SDK 不一致，尝试宽松反序列化", event.getApiVersion());
                return deserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
