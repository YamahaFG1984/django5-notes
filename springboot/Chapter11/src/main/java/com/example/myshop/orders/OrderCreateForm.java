package com.example.myshop.orders;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.example.myshop.common.validation.UsZipCode;

/** class OrderCreateForm(forms.ModelForm): fields = first_name, last_name, email, address, postal_code, city */
public class OrderCreateForm {

    @NotBlank @Size(max = 50)
    private String firstName;

    @NotBlank @Size(max = 50)
    private String lastName;

    @NotBlank @Email @Size(max = 254)
    private String email;

    @NotBlank @Size(max = 250)
    private String address;

    /** postal_code = USZipCodeField() */
    @NotBlank @Size(max = 20) @UsZipCode
    private String postalCode;

    @NotBlank @Size(max = 100)
    private String city;

    public Order toOrder() {
        return new Order(firstName, lastName, email, address, postalCode, city);
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
