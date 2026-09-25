package com.example.myshop.shop.admin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * list_editable = ['price', 'available'] 的数据结构：列表页每一行一个 Row。
 * 请求参数形如 rows[0].id=3&rows[0].price=9.90&rows[0].available=true，
 * Spring 的数据绑定能直接填充 List 里的对象（第 13 章的 formset 也靠它）。
 */
public class ProductListForm {

    @Valid
    private List<Row> rows = new ArrayList<>();

    public static class Row {

        @NotNull
        private Long id;

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal price;

        private boolean available;

        public Row() {
        }

        public Row(Long id, BigDecimal price, boolean available) {
            this.id = id;
            this.price = price;
            this.available = available;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }
}
