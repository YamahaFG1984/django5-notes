package com.example.myshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stripe 配置（≈ STRIPE_PUBLISHABLE_KEY / STRIPE_SECRET_KEY / STRIPE_WEBHOOK_SECRET）。
 * API 版本由 stripe-java 的版本决定（Stripe.API_VERSION），不像 Python SDK 那样在设置里指定。
 */
@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(String publishableKey, String secretKey, String webhookSecret) {

    /** 测试密钥形如 sk_test_...，对应 Django 版 get_stripe_url() 里的 '_test_' in settings.STRIPE_SECRET_KEY */
    public boolean isTestMode() {
        return secretKey != null && secretKey.contains("_test_");
    }
}
