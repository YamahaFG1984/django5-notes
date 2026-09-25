package com.example.mysite;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.test.web.servlet.ResultActions;

/** 测试辅助：用 jsoup 解析响应 HTML，按 CSS 选择器取文本，比 containsString 更精确。 */
public final class HtmlPage {

    private HtmlPage() {
    }

    public static Document parse(ResultActions result) throws Exception {
        return Jsoup.parse(result.andReturn().getResponse().getContentAsString());
    }

    public static List<String> texts(ResultActions result, String cssQuery) throws Exception {
        return parse(result).select(cssQuery).stream().map(Element::text).toList();
    }
}
