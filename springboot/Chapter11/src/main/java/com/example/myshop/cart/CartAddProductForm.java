package com.example.myshop.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * class CartAddProductForm(forms.Form)：
 * quantity = TypedChoiceField(choices=1..20, coerce=int)，override = BooleanField(widget=HiddenInput)
 */
public class CartAddProductForm {

    public static final int MAX_QUANTITY = 20;

    @Min(1)
    @Max(MAX_QUANTITY)
    private int quantity = 1;

    private boolean override;

    public CartAddProductForm() {
    }

    public CartAddProductForm(int quantity, boolean override) {
        this.quantity = quantity;
        this.override = override;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }
}
