package com.example.myshop.i18n;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 翻译过的 URL 片段，≈ path(_('cart/'), ...) 以及 .po 文件里的 msgid "cart/" → msgstr "carro/"。
 * 代码和控制器里始终使用英文（“规范”）片段；只有进出站的 URL 才翻译。
 */
public final class UrlTranslations {

    /** 语言 → (英文片段 → 本地化片段) */
    private static final Map<String, Map<String, String>> TO_LOCAL = Map.of(
            "es", Map.of(
                    "cart", "carro",
                    "orders", "pedidos",
                    "create", "crear",
                    "payment", "pago",
                    "process", "procesar",
                    "completed", "completado",
                    "canceled", "cancelado",
                    "coupons", "cupon"));

    private static final Map<String, Map<String, String>> TO_CANONICAL = TO_LOCAL.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey))));

    private UrlTranslations() {
    }

    /** /carro/add/1/ → /cart/add/1/（进站：交给控制器之前） */
    public static String toCanonical(String language, String path) {
        return translate(path, TO_CANONICAL.getOrDefault(language, Map.of()));
    }

    /** /cart/add/1/ → /carro/add/1/（出站：生成链接和重定向时） */
    public static String toLocal(String language, String path) {
        return translate(path, TO_LOCAL.getOrDefault(language, Map.of()));
    }

    private static String translate(String path, Map<String, String> table) {
        if (table.isEmpty() || path.isEmpty()) {
            return path;
        }
        String[] segments = path.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            segments[i] = table.getOrDefault(segments[i], segments[i]);
        }
        return String.join("/", segments);
    }
}
