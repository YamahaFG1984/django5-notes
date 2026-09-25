package com.example.educa.courses.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * DRF PageNumberPagination 的响应格式：{"count": …, "next": …, "previous": …, "results": […]}。
 * Spring Data 的 Page 序列化出来的结构不同（content、totalElements……），这里转换成 DRF 的样子，
 * 书中的 enroll_all.py 这类客户端就能照原样工作。
 */
public record PageResponse<T>(long count, String next, String previous, List<T> results) {

    public static <T> PageResponse<T> of(Page<T> page) {
        int current = page.getNumber() + 1;   // Spring Data 的页码从 0 开始，API 从 1 开始
        return new PageResponse<>(page.getTotalElements(),
                page.hasNext() ? pageUrl(current + 1) : null,
                page.hasPrevious() ? pageUrl(current - 1) : null,
                page.getContent());
    }

    /** 保留当前请求的其他参数（比如 page_size），只替换 page；DRF 的上一页是第 1 页时会去掉 page 参数 */
    private static String pageUrl(int page) {
        var builder = ServletUriComponentsBuilder.fromCurrentRequest();
        if (page == 1) {
            builder.replaceQueryParam("page");
        } else {
            builder.replaceQueryParam("page", page);
        }
        return builder.build().toUriString();
    }
}
