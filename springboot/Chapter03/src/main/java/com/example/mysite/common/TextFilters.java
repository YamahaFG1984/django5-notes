package com.example.mysite.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Django 内置模板过滤器 linebreaks / truncatewords / truncatewords_html 的替代品。
 * Thymeleaf 没有“过滤器”概念，这里写成普通方法，再由 {@link TemplateHelpersDialect} 暴露为 {@code #text}。
 */
@Component
public class TextFilters {

    private static final Pattern WORD = Pattern.compile("\\S+");

    /** {{ value|linebreaks }}：先转义 HTML，空行分段成 &lt;p&gt;，单个换行变 &lt;br&gt;。 */
    public String linebreaks(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n').strip();
        return Arrays.stream(normalized.split("\n{2,}"))
                .map(paragraph -> "<p>" + HtmlUtils.htmlEscape(paragraph).replace("\n", "<br>") + "</p>")
                .collect(Collectors.joining("\n\n"));
    }

    /** {{ value|truncatewords:n }}：保留前 n 个单词，超出部分用 … 结尾。 */
    public String truncatewords(String value, int count) {
        if (value == null) {
            return "";
        }
        String[] words = value.strip().split("\\s+");
        if (words.length <= count) {
            return value.strip();
        }
        return String.join(" ", Arrays.copyOfRange(words, 0, count)) + " …";
    }

    /**
     * {{ value|truncatewords_html:n }}：只数文本里的单词，截断处补上 …，
     * 已经打开的标签会被正常闭合，不会留下半截 &lt;p&gt;。
     * 实现：用 jsoup 按文档顺序遍历节点，数到第 n 个单词后，把后面的节点全部删掉。
     */
    public String truncatewordsHtml(String html, int count) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document doc = Jsoup.parseBodyFragment(html);
        doc.outputSettings().prettyPrint(false);

        List<Node> following = new ArrayList<>();
        TextNode[] cutNode = new TextNode[1];
        int[] cutAt = new int[1];
        int[] seen = new int[1];
        doc.body().traverse((node, depth) -> {
            if (cutNode[0] != null) {
                following.add(node);
                return;
            }
            if (node instanceof TextNode text) {
                Matcher words = WORD.matcher(text.getWholeText());
                while (words.find()) {
                    if (++seen[0] == count) {
                        cutNode[0] = text;
                        cutAt[0] = words.end();
                        return;
                    }
                }
            }
        });
        if (cutNode[0] == null) {
            return doc.body().html();   // 单词数不够 n 个，原样返回
        }
        String whole = cutNode[0].getWholeText();
        boolean more = !whole.substring(cutAt[0]).isBlank()
                || following.stream().anyMatch(node -> node instanceof TextNode t && !t.isBlank());
        if (!more) {
            return doc.body().html();   // 恰好 n 个单词，不需要省略号
        }
        cutNode[0].text(whole.substring(0, cutAt[0]) + " …");
        following.forEach(node -> {
            if (node.parent() != null) {
                node.remove();
            }
        });
        return doc.body().html();
    }
}
