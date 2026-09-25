package com.example.myshop.orders.admin;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.myshop.common.Messages;
import com.example.myshop.common.NotFoundException;
import com.example.myshop.config.StripeProperties;
import com.example.myshop.orders.InvoicePdfRenderer;
import com.example.myshop.orders.Order;
import com.example.myshop.orders.OrderRepository;

/**
 * 订单后台（orders/admin.py 的 OrderAdmin + orders/views.py 的 admin_order_detail、admin_order_pdf）：
 * 列表 + 过滤 + “导出 CSV”动作 + 自定义详情页 + PDF 发票。
 */
@Controller
@RequestMapping("/admin/orders/order")
public class OrderAdminController {

    private final OrderRepository orders;
    private final OrderCsvExporter csvExporter;
    private final InvoicePdfRenderer invoices;
    private final StripeProperties stripe;

    public OrderAdminController(OrderRepository orders, OrderCsvExporter csvExporter,
                                InvoicePdfRenderer invoices, StripeProperties stripe) {
        this.orders = orders;
        this.csvExporter = csvExporter;
        this.invoices = invoices;
        this.stripe = stripe;
    }

    /** list_display（含 order_payment、order_detail、order_pdf 三个自定义列）+ list_filter = ['paid', ...] */
    @GetMapping("/")
    public String list(@RequestParam(required = false) Boolean paid, Model model) {
        model.addAttribute("orders", paid == null ? orders.findAllByOrderByCreatedDesc() : orders.findByPaidOrderByCreatedDesc(paid));
        model.addAttribute("paid", paid);
        model.addAttribute("stripeTestMode", stripe.isTestMode());
        return "admin/orders/order_list";
    }

    /**
     * 管理动作：勾选若干订单，选择 “Export to CSV”，提交。
     * Django admin 把选中的行作为 QuerySet 传给动作函数；这里是一组 id。
     */
    @PostMapping("/action/")
    @Transactional(readOnly = true)
    public Object action(@RequestParam String action, @RequestParam(name = "ids", required = false) List<Long> ids,
                         HttpServletResponse response, RedirectAttributes redirect) throws IOException {
        if (ids == null || ids.isEmpty()) {
            Messages.info(redirect, "Items must be selected in order to perform actions on them. No items have been changed.");
            return "redirect:/admin/orders/order/";
        }
        if (!"export_to_csv".equals(action)) {
            return ResponseEntity.badRequest().build();
        }
        response.setContentType("text/csv;charset=utf-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("order.csv").build().toString());
        csvExporter.write(orders.findByIdInOrderByIdAsc(ids), response.getWriter());
        return null;   // 已经直接写了响应体
    }

    /** 自定义后台视图 admin_order_detail（书中用 @staff_member_required 保护，这里由 SecurityConfig 统一保护 /admin/**） */
    @GetMapping("/{id}/detail/")
    @Transactional(readOnly = true)
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("order", order(id));
        model.addAttribute("stripeTestMode", stripe.isTestMode());
        return "admin/orders/order_detail";
    }

    /** admin_order_pdf：Content-Disposition: filename=order_1.pdf（inline，浏览器里直接打开） */
    @GetMapping("/{id}/pdf/")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        Order order = order(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("order_" + order.getId() + ".pdf").build().toString())
                .body(invoices.render(order));
    }

    private Order order(Long id) {
        return orders.findWithItemsById(id).orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
    }
}
