package com.itsm.exception;

import org.springframework.http.HttpStatus;

// 400: the request is wrong (bad input, rule violated)
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}