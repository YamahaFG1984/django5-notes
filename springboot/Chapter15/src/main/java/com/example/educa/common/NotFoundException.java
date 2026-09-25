package com.example.educa.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 抛出它就会返回 404，作用等同于 Django 的 get_object_or_404 / Http404。 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    /** ≈ get_object_or_404 抛出的 Http404 */
    public NotFoundException() {
        super("Not found");
    }

    public NotFoundException(String message) {
        super(message);
    }
}
