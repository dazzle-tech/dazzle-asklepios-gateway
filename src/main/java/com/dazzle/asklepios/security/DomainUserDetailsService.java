package com.dazzle.asklepios.security;

import com.dazzle.asklepios.domain.Authority;
import com.dazzle.asklepios.domain.User;
import com.dazzle.asklepios.repository.UserRepository;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class DomainUserDetailsService  {

    private static final Logger LOG = LoggerFactory.getLogger(DomainUserDetailsService.class);

    private final UserRepository userRepository;

    public DomainUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<UserDetails> findByUsernameAndFacility(String login, Long facilityId) {
        LOG.debug("Authenticating {} for facility {}", login, facilityId);

        if (new EmailValidator().isValid(login, null)) {
            return userRepository
                .findOneWithAuthoritiesByEmailIgnoreCaseAndFacilityId(login, facilityId)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User with email " + login + " and facility " + facilityId + " was not found in the database")))
                .map(user -> createSpringSecurityUser(login, user));
        }

        String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
        return userRepository
            .findOneWithAuthoritiesByLoginAndFacilityId(lowercaseLogin, facilityId)
            .switchIfEmpty(Mono.error(new UsernameNotFoundException("User " + lowercaseLogin + " with facility " + facilityId + " was not found in the database")))
            .map(user -> createSpringSecurityUser(lowercaseLogin, user));
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String lowercaseLogin, User user) {
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
        }
        List<SimpleGrantedAuthority> grantedAuthorities = user
            .getAuthorities()
            .stream()
            .map(Authority::getName)
            .map(SimpleGrantedAuthority::new)
            .toList();
        return new org.springframework.security.core.userdetails.User(user.getLogin(), user.getPassword(), grantedAuthorities);
    }
}
