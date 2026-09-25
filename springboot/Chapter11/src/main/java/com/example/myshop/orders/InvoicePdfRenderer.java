package com.example.myshop.orders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import com.example.myshop.i18n.Languages;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

/**
 * HTML → PDF（≈ weasyprint.HTML(string=html).write_pdf(...)）。
 * 两步：① 用 Thymeleaf 把模板渲染成字符串（≈ render_to_string）；② 交给 openhtmltopdf 排版成 PDF。
 * openhtmltopdf 要求 XHTML（标签必须闭合，如 &lt;br/&gt;），并且只支持 CSS 2.1 加少量 CSS3。
 */
@Component
public class InvoicePdfRenderer {

    private final ITemplateEngine templateEngine;

    public InvoicePdfRenderer(ITemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] render(Order order) {
        // 用当前语言渲染（后台里是英文；将来按顾客语言发送发票时，可以在消息里带上语言）
        Context context = new Context(Languages.locale(Languages.current()));
        context.setVariable("order", order);
        String html = templateEngine.process("orders/order/pdf", context);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // 基准地址指向 classpath 的 static/ 目录，模板里的 <link href="css/pdf.css"> 才能找到样式表
            String baseUri = new ClassPathResource("static/").getURL().toExternalForm();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, baseUri);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
