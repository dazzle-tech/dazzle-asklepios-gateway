package com.dazzle.asklepios.config;

import com.dazzle.asklepios.security.AuthoritiesConstants;
import com.dazzle.asklepios.security.CustomAuthenticationProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.web.bind.annotation.CrossOrigin;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

@Configuration
@EnableWebFluxSecurity
 @EnableReactiveMethodSecurity
public class SecurityConfiguration {
    //TODO: maybe move to properties
    private final String contentSecurityPolicy = "default-src 'self'; frame-src 'self' data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://storage.googleapis.com; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Qualifier("customAuthenticationProvider")
    public ReactiveAuthenticationManager authenticationManager(CustomAuthenticationProvider customAuthenticationProvider) {
        return customAuthenticationProvider;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .securityMatcher(
                new NegatedServerWebExchangeMatcher(
                    new OrServerWebExchangeMatcher(pathMatchers("/app/**", "/i18n/**", "/content/**", "/swagger-ui/**"))
                )
            )
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .headers(headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy))
                    .frameOptions(frameOptions -> frameOptions.mode(Mode.DENY))
                    .referrerPolicy(referrer ->
                        referrer.policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                    )
                    .permissionsPolicy(permissions ->
                        permissions.policy(
                            "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"
                        )
                    )
            )
            .authorizeExchange(authz -> authz
                .pathMatchers("/api/authenticate").permitAll()
                .pathMatchers("/api/register").permitAll()
                .pathMatchers("/api/activate").permitAll()
                .pathMatchers("/api/account/reset-password/init").permitAll()
                .pathMatchers("/api/account/reset-password/finish").permitAll()
                .pathMatchers("/v3/api-docs").permitAll()
                .pathMatchers("/api/admin/**").hasAuthority(AuthoritiesConstants.ADMIN)
                .pathMatchers("/api/**").authenticated()
                .pathMatchers("/services/*/management/health/readiness").permitAll()
                .pathMatchers("/services/*/v3/api-docs").hasAuthority(AuthoritiesConstants.ADMIN)
                .pathMatchers("/services/**").authenticated()
                .pathMatchers("/v3/api-docs/**").hasAuthority(AuthoritiesConstants.ADMIN)
                .pathMatchers("/management/health").permitAll()
                .pathMatchers("/management/health/**").permitAll()
                .pathMatchers("/management/info").permitAll()
                .pathMatchers("/management/prometheus").permitAll()
                .pathMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
                .pathMatchers("/reference-data/**").permitAll()
                .pathMatchers("/appointment/**").permitAll()
                .pathMatchers("/attachment/**").permitAll()
                .pathMatchers("/dental/**").permitAll()
                .pathMatchers("/dvm/**").permitAll()
                .pathMatchers("/encounter/**").permitAll()
                .pathMatchers("/general/**").permitAll()
                .pathMatchers("/lab/**").permitAll()
                .pathMatchers("/medications/**").permitAll()
                .pathMatchers("/observation/**").permitAll()
                .pathMatchers("/pas/**").permitAll()
                .pathMatchers("/rad/**").permitAll()
                .pathMatchers("/setup/**").permitAll()
                .pathMatchers("/auth/**").permitAll()
                .anyExchange().authenticated()
            )
            .httpBasic(basic -> basic.disable())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
        return http.build();
    }
}
