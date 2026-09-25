package com.example.myshop.coupons;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.myshop.common.Messages;

/** CouponAdmin：列表（按 active 过滤、按 code 搜索）+ 新增。 */
@Controller
@RequestMapping("/admin/coupons/coupon")
public class CouponAdminController {

    public static class CouponForm {
        @NotBlank @Size(max = 50) private String code;
        @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") private LocalDateTime validFrom;
        @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") private LocalDateTime validTo;
        /** help_text='Percentage value (0 to 100)' */
        @NotNull @Min(0) @Max(100) private Integer discount;
        private boolean active = true;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public LocalDateTime getValidFrom() { return validFrom; }
        public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
        public LocalDateTime getValidTo() { return validTo; }
        public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
        public Integer getDiscount() { return discount; }
        public void setDiscount(Integer discount) { this.discount = discount; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    private final CouponRepository coupons;

    public CouponAdminController(CouponRepository coupons) {
        this.coupons = coupons;
    }

    @GetMapping("/")
    public String list(@RequestParam(required = false) Boolean active, @RequestParam(required = false) String q, Model model) {
        model.addAttribute("coupons", coupons.findAllByOrderByValidToDesc().stream()
                .filter(c -> active == null || c.isActive() == active)
                .filter(c -> q == null || q.isBlank() || c.getCode().toLowerCase().contains(q.strip().toLowerCase()))
                .toList());
        model.addAttribute("active", active);
        model.addAttribute("q", q);
        model.addAttribute("form", new CouponForm());
        return "admin/coupons/coupon_list";
    }

    @PostMapping("/add/")
    public String add(@Valid @ModelAttribute("form") CouponForm form, BindingResult errors, Model model,
                      RedirectAttributes redirect) {
        if (!errors.hasFieldErrors("code") && coupons.existsByCodeIgnoreCase(form.getCode())) {
            errors.rejectValue("code", "unique", "Coupon with this Code already exists.");
        }
        if (!errors.hasFieldErrors("validTo") && form.getValidFrom() != null && form.getValidTo() != null
                && form.getValidTo().isBefore(form.getValidFrom())) {
            errors.rejectValue("validTo", "range", "Valid to must be after valid from.");
        }
        if (errors.hasErrors()) {
            model.addAttribute("coupons", coupons.findAllByOrderByValidToDesc());
            return "admin/coupons/coupon_list";
        }
        coupons.save(new Coupon(form.getCode(), form.getValidFrom().atOffset(ZoneOffset.UTC),
                form.getValidTo().atOffset(ZoneOffset.UTC), form.getDiscount(), form.isActive()));
        Messages.success(redirect, "The coupon “" + form.getCode() + "” was added successfully.");
        return "redirect:/admin/coupons/coupon/";
    }
}
