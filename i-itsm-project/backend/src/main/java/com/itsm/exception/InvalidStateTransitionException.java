package com.itsm.exception;

// 400: the incident lifecycle does not allow this status change
public class InvalidStateTransitionException extends BadRequestException {
    public InvalidStateTransitionException(String from, String to) {
        super("Invalid state transition from " + from + " to " + to);
    }
}