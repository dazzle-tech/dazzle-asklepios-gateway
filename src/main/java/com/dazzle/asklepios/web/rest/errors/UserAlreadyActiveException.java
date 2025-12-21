package com.dazzle.asklepios.web.rest.errors;


public class UserAlreadyActiveException extends RuntimeException {
    public UserAlreadyActiveException() {
        super("USER_ALREADY_ACTIVE");
    }
}
