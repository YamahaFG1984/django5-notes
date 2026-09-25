package com.example.bookmarks.account;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 登录页与修改密码（≈ django.contrib.auth.views 里的 LoginView、PasswordChangeView）。
 * 注意：登录表单的 POST 和登出都由 Spring Security 的过滤器处理，这里只负责“显示页面”。
 */
@Controller
@RequestMapping("/account")
public class AuthController {

    private final AccountService accounts;

    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    /** 登录失败时 Spring Security 会重定向到 /account/login/?error */
    @GetMapping("/login/")
    public String login(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("loginError", error != null);
        return "registration/login";
    }

    @GetMapping("/password-change/")
    public String passwordChangeForm(Model model) {
        model.addAttribute("form", new PasswordChangeForm());
        return "registration/password_change_form";
    }

    @PostMapping("/password-change/")
    public String passwordChange(@AuthenticationPrincipal CurrentUser me,
                                 @Valid @ModelAttribute("form") PasswordChangeForm form, BindingResult errors) {
        if (!errors.hasFieldErrors("oldPassword") && !accounts.checkPassword(me.id(), form.getOldPassword())) {
            errors.rejectValue("oldPassword", "password_incorrect",
                    "Your old password was entered incorrectly. Please enter it again.");
        }
        if (!errors.hasFieldErrors("newPassword2") && !form.passwordsMatch()) {
            errors.rejectValue("newPassword2", "password_mismatch", "The two password fields didn't match.");
        }
        if (errors.hasErrors()) {
            return "registration/password_change_form";
        }
        accounts.setPassword(me.id(), form.getNewPassword1());
        return "redirect:/account/password-change/done/";
    }
}
