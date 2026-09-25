package com.example.bookmarks.common;

import java.util.Map;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@code ${#querystring.with('page', 2)}} → {@code ?q=django&page=2}：
 * 复制当前请求的查询参数，只替换指定的那一个。
 */
public class Querystring {

    private final Map<String, String[]> params;

    Querystring(Map<String, String[]> params) {
        this.params = params;
    }

    public String with(String name, Object value) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        params.forEach((key, values) -> {
            if (!key.equals(name)) {
                builder.queryParam(key, (Object[]) values);
            }
        });
        builder.queryParam(name, value);
        return builder.build().encode().toUriString();
    }
}
