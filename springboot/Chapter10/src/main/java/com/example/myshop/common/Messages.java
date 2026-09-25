package com.example.myshop.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 迷你版的 Django messages 框架。
 * <ul>
 *   <li>重定向前用 {@link #success(RedirectAttributes, String)}：消息存进“闪存属性”，只在下一个请求里可见一次</li>
 *   <li>直接渲染页面时用 {@link #error(Model, String)}：消息放进当前模型</li>
 * </ul>
 * 模板（base.html）统一从名为 messages 的属性里读取并显示。
 */
public final class Messages {

    public static final String ATTRIBUTE = "messages";

    /** 对应 Django 的 message.tags（debug/info/success/warning/error）。 */
    public record Message(String level, String text) {
    }

    private Messages() {
    }

    public static void success(RedirectAttributes redirect, String text) {
        add(redirect.getFlashAttributes(), redirect::addFlashAttribute, "success", text);
    }

    public static void info(RedirectAttributes redirect, String text) {
        add(redirect.getFlashAttributes(), redirect::addFlashAttribute, "info", text);
    }

    public static void success(Model model, String text) {
        add(model.asMap(), model::addAttribute, "success", text);
    }

    public static void error(Model model, String text) {
        add(model.asMap(), model::addAttribute, "error", text);
    }

    @SuppressWarnings("unchecked")
    private static void add(Map<String, ?> current, java.util.function.BiConsumer<String, Object> put,
                            String level, String text) {
        List<Message> list = new ArrayList<>();
        Object existing = current.get(ATTRIBUTE);
        if (existing instanceof List<?> previous) {
            list.addAll((List<Message>) previous);
        }
        list.add(new Message(level, text));
        put.accept(ATTRIBUTE, list);
    }
}
