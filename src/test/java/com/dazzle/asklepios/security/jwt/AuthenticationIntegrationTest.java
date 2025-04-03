package com.dazzle.asklepios.security.jwt;

import com.dazzle.asklepios.config.SecurityConfiguration;
import com.dazzle.asklepios.config.SecurityJwtConfiguration;
import com.dazzle.asklepios.config.WebConfigurer;
import com.dazzle.asklepios.management.SecurityMetersService;
import com.dazzle.asklepios.security.CustomAuthenticationProvider;
import com.dazzle.asklepios.security.DomainUserDetailsService;
import com.dazzle.asklepios.web.rest.AuthenticateController;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(
    {
        CustomAuthenticationProvider.class,
        DomainUserDetailsService.class,
        WebConfigurer.class,
        SecurityConfiguration.class,
        SecurityJwtConfiguration.class,
        SecurityMetersService.class,
        JwtAuthenticationTestUtils.class,
    }
)
@WebFluxTest(
    controllers = { AuthenticateController.class },
    properties = {
        "asklepios.security.authentication.jwt.base64-secret=fd54a45s65fds737b9aafcb3412e07ed99b267f33413274720ddbb7f6c5e64e9f14075f2d7ed041592f0b7657baf8",
        "asklepios.security.authentication.jwt.token-validity-in-seconds=60000",
    }
)
@ComponentScan({})
public @interface AuthenticationIntegrationTest {
}
