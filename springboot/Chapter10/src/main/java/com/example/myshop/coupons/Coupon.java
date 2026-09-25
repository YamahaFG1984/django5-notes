package com.example.myshop.coupons;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 优惠券，对应 coupons/models.py 的 Coupon。discount 是百分比（0–100）。 */
@Entity
@Table(name = "coupons_coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private OffsetDateTime validTo;

    /** validators=[MinValueValidator(0), MaxValueValidator(100)]；数据库里另有 CHECK 约束兜底 */
    @Min(0)
    @Max(100)
    @Column(nullable = false)
    private int discount;

    @Column(nullable = false)
    private boolean active;

    protected Coupon() {
    }

    public Coupon(String code, OffsetDateTime validFrom, OffsetDateTime validTo, int discount, boolean active) {
        this.code = code;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.discount = discount;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public int getDiscount() {
        return discount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return code;
    }
}
