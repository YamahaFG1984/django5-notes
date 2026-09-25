package com.example.bookmarks.account;

import jakarta.validation.Valid;

import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.bookmarks.config.BookmarksProperties;

/**
 * 找回密码的四个步骤，对应 Django 的 PasswordResetView / DoneView / ConfirmView / CompleteView：
 * 输入邮箱 → 提示“已发送” → 点邮件里的链接设置新密码 → 提示“已完成”。
 * “已发送”“已完成”两个纯展示页面在 WebConfig 里用 addViewController 注册。
 */
@Controller
@RequestMapping("/account/password-reset")
public class PasswordResetController {

    private final UserRepository users;
    private final AccountService accounts;
    private final PasswordResetTokens tokens;
    private final MailSender mailSender;
    private final BookmarksProperties properties;

    public PasswordResetController(UserRepository users, AccountService accounts, PasswordResetTokens tokens,
                                   MailSender mailSender, BookmarksProperties properties) {
        this.users = users;
        this.accounts = accounts;
        this.tokens = tokens;
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @GetMapping("/")
    public String form(Model model) {
        model.addAttribute("form", new PasswordResetForm());
        return "registration/password_reset_form";
    }

    /** 无论邮箱是否存在都跳到同一个页面，避免被用来探测哪些邮箱注册过。 */
    @PostMapping("/")
    public String send(@Valid @ModelAttribute("form") PasswordResetForm form, BindingResult errors) {
        if (errors.hasErrors()) {
            return "registration/password_reset_form";
        }
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        for (User user : users.findByEmailIgnoreCaseAndActiveTrue(form.getEmail())) {
            String link = "%s/account/password-reset/%s/%s/".formatted(baseUrl, tokens.uid(user), tokens.make(user));
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.defaultFromEmail());
            message.setTo(user.getEmail());
            message.setSubject("Password reset on Bookmarks");
            // registration/password_reset_email.html 的内容
            message.setText("""
                    Someone asked for password reset for email %s. Follow the link below:
                    %s
                    Your username, in case you've forgotten: %s
                    """.formatted(user.getEmail(), link, user.getUsername()));
            mailSender.send(message);
        }
        return "redirect:/account/password-reset/done/";
    }

    @GetMapping("/{uidb64}/{token}/")
    public String confirmForm(@PathVariable String uidb64, @PathVariable String token, Model model) {
        model.addAttribute("validlink", resolve(uidb64, token) != null);
        model.addAttribute("form", new SetPasswordForm());
        return "registration/password_reset_confirm";
    }

    @PostMapping("/{uidb64}/{token}/")
    public String confirm(@PathVariable String uidb64, @PathVariable String token,
                          @Valid @ModelAttribute("form") SetPasswordForm form, BindingResult errors, Model model) {
        User user = resolve(uidb64, token);
        model.addAttribute("validlink", user != null);
        if (user == null) {
            return "registration/password_reset_confirm";
        }
        if (!errors.hasFieldErrors("newPassword2") && !form.passwordsMatch()) {
            errors.rejectValue("newPassword2", "password_mismatch", "The two password fields didn't match.");
        }
        if (errors.hasErrors()) {
            return "registration/password_reset_confirm";
        }
        accounts.setPassword(user.getId(), form.getNewPassword1());   // 密码哈希变了，这个链接随之失效
        return "redirect:/account/password-reset/complete/";
    }

    private User resolve(String uidb64, String token) {
        return tokens.decodeUid(uidb64)
                .flatMap(users::findById)
                .filter(User::isActive)
                .filter(user -> tokens.check(user, token))
                .orElse(null);
    }
}
