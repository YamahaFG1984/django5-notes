package com.example.bookmarks.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册表单，对应 Django 的 UserRegistrationForm（ModelForm: username, first_name, email + 两个密码字段）。
 * ModelForm 会自动检查“用户名已存在”，clean_password2 检查两次密码一致 —— 这两项在 AccountController 里做。
 */
public class UserRegistrationForm {

    /** Django 的 UnicodeUsernameValidator：字母、数字和 @ . + - _ */
    @NotBlank
    @Size(max = 150)
    @Pattern(regexp = "[\\w.@+-]+", message = "Enter a valid username. This value may contain only letters, numbers, and @/./+/-/_ characters.")
    private String username;

    @Size(max = 150)
    private String firstName = "";

    @Email
    @Size(max = 254)
    private String email = "";

    /** 书中的表单没有做密码强度校验；这里至少要求 8 位（≈ MinimumLengthValidator）。 */
    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank
    private String password2;

    public boolean passwordsMatch() {
        return password != null && password.equals(password2);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }
}
