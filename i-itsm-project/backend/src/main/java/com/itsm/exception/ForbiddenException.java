package com.itsm.exception;

import org.springframework.http.HttpStatus;

// 403: signed in, but not allowed to do this
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}