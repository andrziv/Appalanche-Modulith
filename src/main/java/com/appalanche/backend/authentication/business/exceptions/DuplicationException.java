package com.appalanche.backend.authentication.business.exceptions;

public class DuplicationException extends RuntimeException {
    public DuplicationException(String message) {
        super(message);
    }
}
