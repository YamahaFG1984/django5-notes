package com.example.myshop.orders.admin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import com.example.myshop.orders.Order;

/**
 * 管理动作 export_to_csv。
 * Django 版通过 opts.get_fields() 反射出模型的所有字段，任何 ModelAdmin 都能复用；
 * 这里为订单显式列出要导出的列 —— 更啰嗦，但列名、顺序、格式完全可控。
 */
@Component
public class OrderCsvExporter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void write(List<Order> orders, Writer writer) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("ID", "first name", "last name", "email", "address", "postal code", "city",
                        "created", "updated", "paid", "stripe id")
                .get();
        try (CSVPrinter csv = new CSVPrinter(writer, format)) {
            for (Order o : orders) {
                csv.printRecord(o.getId(), o.getFirstName(), o.getLastName(), o.getEmail(), o.getAddress(),
                        o.getPostalCode(), o.getCity(), o.getCreated().format(DATE), o.getUpdated().format(DATE),
                        o.isPaid(), o.getStripeId());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
