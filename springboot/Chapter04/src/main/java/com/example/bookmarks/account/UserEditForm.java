package com.example.bookmarks.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** class UserEditForm(forms.ModelForm): fields = ['first_name', 'last_name', 'email'] */
public class UserEditForm {

    @Size(max = 150)
    private String firstName = "";

    @Size(max = 150)
    private String lastName = "";

    @Email
    @Size(max = 254)
    private String email = "";

    public static UserEditForm of(User user) {
        UserEditForm form = new UserEditForm();
        form.firstName = user.getFirstName();
        form.lastName = user.getLastName();
        form.email = user.getEmail();
        return form;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
