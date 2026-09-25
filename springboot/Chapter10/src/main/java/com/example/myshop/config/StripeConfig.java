package com.example.myshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stripe.StripeClient;

/**
 * 书中用全局变量 stripe.api_key = ...；stripe-java 推荐创建一个 StripeClient 实例，
 * 在 Spring 里把它注册成 Bean，需要的地方注入即可，也方便在测试里替换。
 */
@Configuration
public class StripeConfig {

    @Bean
    StripeClient stripeClient(StripeProperties properties) {
        return new StripeClient(properties.secretKey());
    }
}
