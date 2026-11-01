package com.itsm.exception;

import org.springframework.http.HttpStatus;

// 401: not signed in, wrong password, or token expired
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}