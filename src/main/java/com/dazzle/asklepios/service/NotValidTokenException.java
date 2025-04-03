package com.dazzle.asklepios.service;

import org.springframework.security.core.AuthenticationException;

public class NotValidTokenException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public NotValidTokenException(String message) {
        super(message);
    }

    public NotValidTokenException(String message, Throwable t) {
        super(message, t);
    }
}
