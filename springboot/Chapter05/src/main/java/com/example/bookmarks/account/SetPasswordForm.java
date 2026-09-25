package com.example.bookmarks.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** django.contrib.auth.forms.SetPasswordForm：重置密码时只填两次新密码。 */
public class SetPasswordForm {

    @NotBlank
    @Size(min = 8, max = 128)
    private String newPassword1;

    @NotBlank
    private String newPassword2;

    public boolean passwordsMatch() {
        return newPassword1 != null && newPassword1.equals(newPassword2);
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
