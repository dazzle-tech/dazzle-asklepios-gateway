package com.dazzle.asklepios.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Setter
@Getter
public class CustomAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private final Long facilityId;

    public CustomAuthenticationToken(Object principal, Object credentials, Long facilityId) {
        super(principal, credentials);
        this.facilityId = facilityId;
    }
}

