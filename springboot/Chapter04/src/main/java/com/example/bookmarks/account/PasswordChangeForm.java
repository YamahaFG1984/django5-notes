package com.example.bookmarks.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** django.contrib.auth.forms.PasswordChangeForm：旧密码 + 两次新密码。 */
public class PasswordChangeForm {

    @NotBlank
    private String oldPassword;

    @NotBlank
    @Size(min = 8, max = 128)
    private String newPassword1;

    @NotBlank
    private String newPassword2;

    public boolean passwordsMatch() {
        return newPassword1 != null && newPassword1.equals(newPassword2);
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword1() {
        return newPassword1;
    }

    public void setNewPassword1(String newPassword1) {
        this.newPassword1 = newPassword1;
    }

    public String getNewPassword2() {
        return newPassword2;
    }

    public void setNewPassword2(String newPassword2) {
        this.newPassword2 = newPassword2;
    }
}
