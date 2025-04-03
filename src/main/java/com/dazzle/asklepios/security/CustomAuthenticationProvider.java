package com.dazzle.asklepios.security;


import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CustomAuthenticationProvider implements ReactiveAuthenticationManager {

    private final DomainUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthenticationProvider(DomainUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (authentication instanceof CustomAuthenticationToken customToken) {

            String username = customToken.getName();
            Long facilityId = customToken.getFacilityId();
            String rawPassword = (String) customToken.getCredentials();

            return userDetailsService.findByUsernameAndFacility(username, facilityId)
                .map(userDetails -> {

                    if (passwordEncoder.matches(rawPassword, userDetails.getPassword())) {
                        return (Authentication) new UsernamePasswordAuthenticationToken(userDetails, authentication.getCredentials(), userDetails.getAuthorities());
                    }
                    else{
                        throw new BadCredentialsException("Invalid credentials");
                    }
                })
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found")))
                .onErrorMap(UserNotActivatedException.class, ex -> {
                    throw ex;
                });
        }

        return Mono.empty();
    }
}

