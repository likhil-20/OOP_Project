package com.itsm.dto;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
