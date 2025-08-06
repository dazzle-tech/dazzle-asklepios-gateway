package com.dazzle.asklepios.web.rest;

import com.dazzle.asklepios.IntegrationTest;
import com.dazzle.asklepios.web.rest.vm.LoginVM;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Integration tests for the {@link AuthenticateController} REST controller.
 */
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_TIMEOUT)
@IntegrationTest
class AuthenticateControllerIT {

    @Autowired
    private ObjectMapper om;

    @Autowired
    private WebTestClient webTestClient;

//    @Test
//    @Ignore
//    void testAuthorize() throws Exception {
//        Mono.delay(Duration.ofSeconds(7))
//            .block();
//        LoginVM login = new LoginVM();
//        login.setUsername("admin");
//        login.setPassword("admin");
//        login.setFacilityId(1L);
//
//        webTestClient
//            .post()
//            .uri("/api/authenticate")
//            .contentType(MediaType.APPLICATION_JSON)
//            .bodyValue(om.writeValueAsBytes(login))
//            .exchange()
//            .expectStatus()
//            .isOk()
//            .expectHeader()
//            .valueMatches("Authorization", "Bearer .+")
//            .expectBody()
//            .jsonPath("$.id_token")
//            .isNotEmpty();
//    }
//
//    @Test
//    @Ignore
//    void testAuthorizeWithRememberMe() throws Exception {
//        Mono.delay(Duration.ofSeconds(7))
//            .block();
//        LoginVM login = new LoginVM();
//        login.setUsername("admin");
//        login.setPassword("admin");
//        login.setFacilityId(1L);
//        login.setRememberMe(true);
//        webTestClient
//            .post()
//            .uri("/api/authenticate")
//            .contentType(MediaType.APPLICATION_JSON)
//            .bodyValue(om.writeValueAsBytes(login))
//            .exchange()
//            .expectStatus()
//            .isOk()
//            .expectHeader()
//            .valueMatches("Authorization", "Bearer .+")
//            .expectBody()
//            .jsonPath("$.id_token")
//            .isNotEmpty();
//    }

    @Test
    void testAuthorizeFails() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername("wrong-user");
        login.setPassword("wrong password");
        login.setFacilityId(1L);
        webTestClient
            .post()
            .uri("/api/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(login))
            .exchange()
            .expectStatus()
            .isUnauthorized()
            .expectHeader()
            .doesNotExist("Authorization")
            .expectBody()
            .jsonPath("$.id_token")
            .doesNotExist();
    }

    @Test
    void testAuthorizeFailsWrongPass() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername("admin");
        login.setPassword("wrong password");
        login.setFacilityId(1L);
        webTestClient
            .post()
            .uri("/api/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(login))
            .exchange()
            .expectStatus()
            .isUnauthorized()
            .expectHeader()
            .doesNotExist("Authorization")
            .expectBody()
            .jsonPath("$.id_token")
            .doesNotExist();
    }
}
