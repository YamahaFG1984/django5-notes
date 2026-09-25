package com.example.myshop.orders;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.myshop.cart.Cart;
import com.example.myshop.cart.CartService;

/** orders/views.py 的 order_create */
@Controller
@RequestMapping("/orders")
public class OrderController {

    public static final String ORDER_ID_SESSION_KEY = "order_id";

    private final OrderService orderService;
    private final CartService cartService;
    private final Cart cart;

    public OrderController(OrderService orderService, CartService cartService, Cart cart) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.cart = cart;
    }

    @GetMapping("/create/")
    public String createForm(Model model) {
        if (cart.isEmpty()) {
            return "redirect:/cart/";   // 书中允许用空购物车下单，这里拦掉
        }
        model.addAttribute("form", new OrderCreateForm());
        model.addAttribute("lines", cartService.lines());
        model.addAttribute("totals", cartService.totals());
        return "orders/order/create";
    }

    /** 下单后把订单 id 放进会话（request.session['order_id'] = order.id），再跳转到支付页 */
    @PostMapping("/create/")
    public String create(@Valid @ModelAttribute("form") OrderCreateForm form, BindingResult errors,
                         Model model, HttpSession session) {
        if (cart.isEmpty()) {
            return "redirect:/cart/";
        }
        if (errors.hasErrors()) {
            model.addAttribute("lines", cartService.lines());
            model.addAttribute("totals", cartService.totals());
            return "orders/order/create";
        }
        Order order = orderService.create(form);
        session.setAttribute(ORDER_ID_SESSION_KEY, order.getId());
        return "redirect:/payment/process/";
    }
}
