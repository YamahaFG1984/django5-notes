package com.example.myshop.payment;

import com.example.myshop.orders.Order;

/**
 * 对外部支付服务的抽象。控制器只依赖这个接口：生产环境调用 Stripe，测试里换成假的实现。
 * （Python 里常用 unittest.mock.patch('stripe.checkout.Session.create') 做同样的事。）
 */
public interface StripeGateway {

    /** 创建 Checkout Session，返回 Stripe 托管支付页的地址 */
    String createCheckoutSession(Order order, String successUrl, String cancelUrl);
}
