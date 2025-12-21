package com.dazzle.asklepios.web.rest.errors;

import com.dazzle.asklepios.web.rest.errors.UserAlreadyActiveException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class AccountExceptionHandler {

    @ExceptionHandler(UserAlreadyActiveException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<ErrorVM> handleUserAlreadyActive(UserAlreadyActiveException ex) {
        return Mono.just(new ErrorVM("USER_ALREADY_ACTIVE"));
    }

    public record ErrorVM(String message) {}
}
