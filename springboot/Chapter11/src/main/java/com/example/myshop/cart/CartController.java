package com.example.myshop.cart;

import java.util.stream.IntStream;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.myshop.common.NotFoundException;
import com.example.myshop.shop.Product;
import com.example.myshop.shop.ProductRepository;
import com.example.myshop.shop.Recommender;

/** cart/views.py + cart/urls.py */
@Controller
@RequestMapping("/cart")
public class CartController {

    private final Cart cart;
    private final CartService cartService;
    private final ProductRepository products;
    private final Recommender recommender;

    public CartController(Cart cart, CartService cartService, ProductRepository products, Recommender recommender) {
        this.cart = cart;
        this.cartService = cartService;
        this.products = products;
        this.recommender = recommender;
    }

    /** @require_POST def cart_add(request, product_id) */
    @PostMapping("/add/{productId}/")
    public String add(@PathVariable Long productId, @Valid @ModelAttribute CartAddProductForm form, BindingResult errors) {
        Product product = products.findById(productId)
                .filter(Product::isAvailable)   // 书中没有检查 available：下架的商品也能加进购物车
                .orElseThrow(() -> new NotFoundException("No Product matches the given query."));
        if (!errors.hasErrors()) {
            cart.add(product, form.getQuantity(), form.isOverride());
        }
        return "redirect:/cart/";
    }

    @PostMapping("/remove/{productId}/")
    public String remove(@PathVariable Long productId) {
        cart.remove(productId);
        return "redirect:/cart/";
    }

    @GetMapping("/")
    public String detail(Model model) {
        var lines = cartService.lines();
        model.addAttribute("lines", lines);
        model.addAttribute("totals", cartService.totals());
        model.addAttribute("recommendedProducts",
                recommender.suggestProductsFor(lines.stream().map(line -> line.product().getId()).toList(), 4));
        model.addAttribute("quantityChoices", IntStream.rangeClosed(1, CartAddProductForm.MAX_QUANTITY).boxed().toList());
        return "cart/detail";
    }
}
