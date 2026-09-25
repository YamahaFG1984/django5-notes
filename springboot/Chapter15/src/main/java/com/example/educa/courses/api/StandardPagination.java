package com.example.educa.courses.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * ≈ StandardPagination(PageNumberPagination)：page_size = 10，可用 ?page_size= 调整，最大 50。
 * 页码不合法时抛 {@link InvalidPageException}，返回 404 {"detail": "Invalid page."}，与 DRF 相同。
 */
public final class StandardPagination {

    public static final int PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;

    private StandardPagination() {
    }

    public static Pageable pageable(String page, String pageSize) {
        int size = PAGE_SIZE;
        if (pageSize != null) {
            try {
                int requested = Integer.parseInt(pageSize);
                if (requested > 0) {
                    size = Math.min(requested, MAX_PAGE_SIZE);
                }
            } catch (NumberFormatException ignored) {
                // DRF 同样忽略不合法的 page_size，使用默认值
            }
        }
        int number = 1;
        if (page != null) {
            try {
                number = Integer.parseInt(page);
            } catch (NumberFormatException e) {
                throw new InvalidPageException();
            }
            if (number < 1) {
                throw new InvalidPageException();
            }
        }
        return PageRequest.of(number - 1, size);
    }

    /** 请求的页码超出了总页数（第 1 页永远合法，即使结果为空） */
    public static void check(Page<?> page) {
        if (page.getNumber() > 0 && page.getNumber() >= page.getTotalPages()) {
            throw new InvalidPageException();
        }
    }

    public static class InvalidPageException extends RuntimeException {
        public InvalidPageException() {
            super("Invalid page.");
        }
    }
}
