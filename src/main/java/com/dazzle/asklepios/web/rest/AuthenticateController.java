package com.dazzle.asklepios.web.rest;

import com.dazzle.asklepios.domain.User;
import com.dazzle.asklepios.domain.enumeration.SecurityLevel;
import com.dazzle.asklepios.repository.UserRepository;
import com.dazzle.asklepios.security.CustomAuthenticationToken;
import com.dazzle.asklepios.web.rest.vm.LoginVM;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import static com.dazzle.asklepios.security.SecurityUtils.AUTHORITIES_KEY;
import static com.dazzle.asklepios.security.SecurityUtils.FACILITY_KEY;
import static com.dazzle.asklepios.security.SecurityUtils.JWT_ALGORITHM;
import static com.dazzle.asklepios.security.SecurityUtils.SECURITY_ACCESS_LEVE;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
public class AuthenticateController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticateController.class);

    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    @Value("${asklepios.security.authentication.jwt.token-validity-in-seconds:0}")
    private long tokenValidityInSeconds;

    @Value("${asklepios.security.authentication.jwt.token-validity-in-seconds-for-remember-me:0}")
    private long tokenValidityInSecondsForRememberMe;

    private final ReactiveAuthenticationManager authenticationManager;

    public AuthenticateController(JwtEncoder jwtEncoder, UserRepository userRepository, ReactiveAuthenticationManager authenticationManager) {
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/authenticate")
    public Mono<ResponseEntity<JWTToken>> authorize(@Valid @RequestBody Mono<LoginVM> loginVM) {
        return loginVM
            .flatMap(login ->
                authenticationManager
                    .authenticate(new CustomAuthenticationToken(login.getUsername(), login.getPassword(), login.getFacilityId()))
                    .flatMap(auth ->
                        Mono.fromCallable(() ->
                            this.createToken(auth, login.isRememberMe(), login.getFacilityId())
                        ).subscribeOn(Schedulers.boundedElastic())
                    )
            )
            .map(jwt -> {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setBearerAuth(jwt);
                return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
            });
    }

    /**
     * {@code GET /authenticate} : check if the user is authenticated, and return its login.
     *
     * @param principal the authentication principal.
     * @return the login if the user is authenticated.
     */
    @GetMapping(value = "/authenticate", produces = MediaType.TEXT_PLAIN_VALUE)
    public String isAuthenticated(Principal principal) {
        LOG.debug("REST request to check if the current user is authenticated");
        return principal == null ? null : principal.getName();
    }
    public String createToken(Authentication authentication, boolean rememberMe, Long facilityId) {
        String login = authentication.getName();

        // Mono<User> -> User باستخدام block()
        com.dazzle.asklepios.domain.User user = userRepository
            .findOneByLogin(login)                      // Mono<User>
            .blockOptional()                            // Optional<User>
            .orElseThrow(() -> new IllegalStateException("User not found: " + login));

        var accessLevel = user.getSecurityAccessLeve(); // Enum أو String

        String authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(" "));

        Instant now = Instant.now();
        Instant validity = rememberMe
            ? now.plus(this.tokenValidityInSecondsForRememberMe, ChronoUnit.SECONDS)
            : now.plus(this.tokenValidityInSeconds, ChronoUnit.SECONDS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(authentication.getName())
            .claim(AUTHORITIES_KEY, authorities)
            .claim(FACILITY_KEY, facilityId)
            .claim(SECURITY_ACCESS_LEVE, accessLevel.toString())
            .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}
