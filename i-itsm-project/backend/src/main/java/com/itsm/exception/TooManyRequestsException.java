package com.itsm.exception;

import org.springframework.http.HttpStatus;

// 429: too many failed login attempts
public class TooManyRequestsException extends ApiException {
    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}