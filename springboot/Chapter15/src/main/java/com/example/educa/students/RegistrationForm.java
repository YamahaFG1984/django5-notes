package com.example.educa.students;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ≈ django.contrib.auth.forms.UserCreationForm：用户名 + 两次密码。
 * 密码规则对应 settings.AUTH_PASSWORD_VALIDATORS 里的 MinimumLength 和 Numeric 两个校验器。
 */
public class RegistrationForm {

    @NotBlank
    @Size(max = 150)
    @Pattern(regexp = "[\\w.@+-]*", message = "Enter a valid username. This value may contain only letters, "
            + "numbers, and @/./+/-/_ characters.")
    private String username;

    @NotBlank
    @Size(min = 8, message = "This password is too short. It must contain at least 8 characters.")
    @Pattern(regexp = ".*\\D.*", message = "This password is entirely numeric.")
    private String password1;

    @NotBlank
    private String password2;

    /** clean_password2()：两次输入必须一致（控制器把错误挂在 password2 字段上） */
    public boolean passwordsMatch() {
        return password1 == null || password1.equals(password2);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword1() {
        return password1;
    }

    public void setPassword1(String password1) {
        this.password1 = password1;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }
}
