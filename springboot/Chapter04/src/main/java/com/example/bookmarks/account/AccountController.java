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

import com.example.bookmarks.common.MediaStorage.InvalidImageException;

/** account/views.py 里的 dashboard、register、edit。 */
@Controller
@RequestMapping("/account")
public class AccountController {

    private final AccountService accounts;
    private final UserRepository users;

    public AccountController(AccountService accounts, UserRepository users) {
        this.accounts = accounts;
        this.users = users;
    }

    /** @login_required 由 SecurityConfig 的 anyRequest().authenticated() 统一保证 */
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("section", "dashboard");
        return "account/dashboard";
    }

    @GetMapping("/register/")
    public String registerForm(Model model) {
        model.addAttribute("userForm", new UserRegistrationForm());
        return "account/register";
    }

    @PostMapping("/register/")
    public String register(@Valid @ModelAttribute("userForm") UserRegistrationForm form, BindingResult errors,
                           Model model) {
        // ModelForm 自动做的唯一性检查
        if (!errors.hasFieldErrors("username") && users.existsByUsername(form.getUsername())) {
            errors.rejectValue("username", "unique", "A user with that username already exists.");
        }
        // def clean_password2(self)
        if (!errors.hasFieldErrors("password2") && !form.passwordsMatch()) {
            errors.rejectValue("password2", "mismatch", "Passwords don't match.");
        }
        if (errors.hasErrors()) {
            return "account/register";
        }
        model.addAttribute("newUser", accounts.register(form));
        return "account/register_done";
    }

    @GetMapping("/edit/")
    public String editForm(@AuthenticationPrincipal CurrentUser me, Model model) {
        model.addAttribute("userForm", UserEditForm.of(accounts.user(me.id())));
        model.addAttribute("profileForm", ProfileEditForm.of(accounts.profileOf(me.id())));
        return renderEdit(me, model);
    }

    /**
     * 一个 HTML 表单，绑定到两个表单对象 —— 和 Django 视图里同时处理 user_form、profile_form 一样。
     * 每个 @ModelAttribute 后面紧跟自己的 BindingResult。
     */
    @PostMapping("/edit/")
    public String edit(@AuthenticationPrincipal CurrentUser me,
                       @Valid @ModelAttribute("userForm") UserEditForm userForm, BindingResult userErrors,
                       @Valid @ModelAttribute("profileForm") ProfileEditForm profileForm, BindingResult profileErrors,
                       Model model) {
        if (!userErrors.hasErrors() && !profileErrors.hasErrors()) {
            try {
                accounts.updateAccount(me.id(), userForm, profileForm);
            } catch (InvalidImageException e) {
                profileErrors.rejectValue("photo", "invalid_image", e.getMessage());
            }
        }
        return renderEdit(me, model);
    }

    private String renderEdit(CurrentUser me, Model model) {
        model.addAttribute("profile", accounts.profileOf(me.id()));
        return "account/edit";
    }
}
