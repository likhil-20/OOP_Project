package com.itsm.exception;

import org.springframework.http.HttpStatus;

// 409: clashes with existing data (e.g. email already registered)
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}