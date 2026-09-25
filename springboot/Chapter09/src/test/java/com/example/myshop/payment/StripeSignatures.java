package com.example.myshop.payment;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** 测试辅助：按 Stripe 的规则给 webhook 请求体签名（t=时间戳,v1=HMAC-SHA256(secret, "时间戳.请求体")）。 */
final class StripeSignatures {

    private StripeSignatures() {
    }

    static String sign(String payload, String secret) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + signature;
    }

    /** 一个最小的 checkout.session.completed 事件；api_version 与 SDK 一致，才能被“安全”反序列化 */
    static String checkoutCompleted(Long orderId, String paymentIntent) {
        return """
                {
                  "id": "evt_test_1",
                  "object": "event",
                  "api_version": "%s",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_1",
                      "object": "checkout.session",
                      "mode": "payment",
                      "payment_status": "paid",
                      "client_reference_id": "%d",
                      "payment_intent": "%s"
                    }
                  }
                }
                """.formatted(com.stripe.Stripe.API_VERSION, orderId, paymentIntent);
    }
}
