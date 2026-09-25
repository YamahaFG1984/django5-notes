package com.example.educa.courses.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.educa.common.NotFoundException;

/**
 * API 的错误也返回 JSON，格式与 DRF 一致：{"detail": "..."}。
 * 只作用于本包里的控制器，网页部分仍然显示 HTML 错误页。
 */
@RestControllerAdvice(basePackageClasses = CourseApiController.class)
public class ApiExceptionHandler {

    @ExceptionHandler({NotFoundException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound() {
        return Map.of("detail", "No object matches the given query.");
    }

    @ExceptionHandler(StandardPagination.InvalidPageException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> invalidPage(StandardPagination.InvalidPageException e) {
        return Map.of("detail", e.getMessage());
    }

    @ExceptionHandler(CourseApiService.NotEnrolledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> forbidden(CourseApiService.NotEnrolledException e) {
        return Map.of("detail", e.getMessage());
    }
}
