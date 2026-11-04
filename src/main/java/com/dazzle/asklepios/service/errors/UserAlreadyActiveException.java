package com.dazzle.asklepios.service.errors;

public class UserAlreadyActiveException extends RuntimeException {
    public UserAlreadyActiveException() {
        super("USER_ALREADY_ACTIVE");
    }
}
