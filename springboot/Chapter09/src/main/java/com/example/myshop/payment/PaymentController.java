package com.example.myshop.payment;

import java.net.URI;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.myshop.common.NotFoundException;
import com.example.myshop.orders.Order;
import com.example.myshop.orders.OrderController;
import com.example.myshop.orders.OrderRepository;

/** payment/views.py：payment_process、payment_completed、payment_canceled */
@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final OrderRepository orders;
    private final StripeGateway stripe;

    public PaymentController(OrderRepository orders, StripeGateway stripe) {
        this.orders = orders;
        this.stripe = stripe;
    }

    @GetMapping("/process/")
    public String processForm(HttpSession session, Model model) {
        model.addAttribute("order", currentOrder(session));
        return "payment/process";
    }

    /**
     * 创建 Stripe Checkout Session，然后用 303 重定向到 Stripe 的支付页（redirect(session.url, code=303)）。
     * Spring 的 "redirect:" 前缀发的是 302，需要特定状态码时直接返回 ResponseEntity。
     */
    @PostMapping("/process/")
    public ResponseEntity<Void> process(HttpSession session) {
        Order order = currentOrder(session);
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        String url = stripe.createCheckoutSession(order, base + "/payment/completed/", base + "/payment/canceled/");
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(url)).build();
    }

    @GetMapping("/completed/")
    public String completed() {
        return "payment/completed";
    }

    @GetMapping("/canceled/")
    public String canceled() {
        return "payment/canceled";
    }

    private Order currentOrder(HttpSession session) {
        Object orderId = session.getAttribute(OrderController.ORDER_ID_SESSION_KEY);
        if (!(orderId instanceof Long id)) {
            throw new NotFoundException("No order in session");
        }
        return orders.findWithItemsById(id).orElseThrow(() -> new NotFoundException("No Order matches the given query."));
    }
}
