package com.example.myshop.coupons;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;

import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.myshop.cart.Cart;

/** coupons/views.py 的 coupon_apply：有效就把 coupon_id 存进会话，无效就清掉。 */
@Controller
public class CouponController {

    /** class CouponApplyForm(forms.Form): code = forms.CharField() */
    public record CouponApplyForm(@NotBlank String code) {
    }

    private final CouponRepository coupons;
    private final Cart cart;

    public CouponController(CouponRepository coupons, Cart cart) {
        this.coupons = coupons;
        this.cart = cart;
    }

    @PostMapping("/coupons/apply/")
    public String apply(@Validated @ModelAttribute CouponApplyForm form) {
        cart.setCouponId(coupons.findValid(form.code().strip(), OffsetDateTime.now())
                .map(Coupon::getId)
                .orElse(null));
        return "redirect:/cart/";
    }
}
