package com.dazzle.asklepios.web.rest;

import com.dazzle.asklepios.IntegrationTest;
import com.dazzle.asklepios.config.Constants;
import com.dazzle.asklepios.domain.User;
import com.dazzle.asklepios.repository.AuthorityRepository;
import com.dazzle.asklepios.repository.UserRepository;
import com.dazzle.asklepios.service.UserService;
import com.dazzle.asklepios.service.dto.CreatePasswordKeyValidationDTO;
import com.dazzle.asklepios.web.rest.vm.KeyAndPasswordVM;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link AccountResource} create-password endpoints.
 */
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_TIMEOUT)
@IntegrationTest
class AccountResourceIT {

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebTestClient accountWebTestClient;

    @Autowired
    private DatabaseClient databaseClient;
    @Autowired
    private UserService userService;

    @AfterEach
    void cleanupAndCheck() {
        String deleteUserRole = "DELETE FROM user_role WHERE user_id IN (SELECT id FROM app_user)";
        databaseClient.sql(deleteUserRole).fetch().rowsUpdated().block();
        userRepository.deleteAll().block();
    }

    @Test
    void testValidateCreatePasswordTokenNotFound() {
        accountWebTestClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/account/create-password/validate")
                .queryParam("key", "missing-key")
                .build()
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody(CreatePasswordKeyValidationDTO.class)
            .value(dto -> {
                assertThat(dto).isNotNull();
                assertThat(dto.valid()).isFalse();
                assertThat(dto.message()).isEqualTo("TOKEN_NOT_FOUND");
            });
    }

    @Test
    void testValidateCreatePasswordOk() {
        User user = new User();
        user.setLogin("cpw-ok"); // <= 20
        user.setEmail("cpw-ok@example.com");
        user.setLangKey("en");
        user.setCreatedBy(Constants.SYSTEM);
        user.setActivated(false);
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setResetKey("CPWKEYOK");
        user.setResetDate(Instant.now().minusSeconds(60));
        userRepository.save(user).block();

        accountWebTestClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/account/create-password/validate")
                .queryParam("key", "CPWKEYOK")
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.valid").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("OK");

        userService.deleteUser("cpw-ok").block();
    }

    @Test
    void testValidateCreatePasswordExpired() {
        User user = new User();
        user.setLogin("cpw-exp"); // <= 20
        user.setEmail("cpw-exp@example.com");
        user.setLangKey("en");
        user.setCreatedBy(Constants.SYSTEM);
        user.setActivated(false);
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setResetKey("CPWKEYEXP");
        user.setResetDate(Instant.now().minus(26, ChronoUnit.HOURS));
        userRepository.save(user).block();

        accountWebTestClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/account/create-password/validate")
                .queryParam("key", "CPWKEYEXP")
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.valid").isEqualTo(false)
            .jsonPath("$.message").isEqualTo("TOKEN_INVALID_OR_EXPIRED");

        userService.deleteUser("cpw-exp").block();
    }

    @Test
    void testFinishCreatePasswordSuccess() throws Exception {
        User user = new User();
        user.setLogin("cpw-fin"); // <= 20
        user.setEmail("cpw-fin@example.com");
        user.setLangKey("en");
        user.setCreatedBy(Constants.SYSTEM);
        user.setActivated(false);
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setResetKey("CPWKEYFIN");
        user.setResetDate(Instant.now().minusSeconds(60));
        userRepository.save(user).block();

        KeyAndPasswordVM body = new KeyAndPasswordVM();
        body.setKey("CPWKEYFIN");
        body.setNewPassword("StrongPass1!");

        accountWebTestClient
            .post()
            .uri("/api/account/create-password/finish")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(body))
            .exchange()
            .expectStatus().isOk();

        User updated = userRepository.findOneByLogin("cpw-fin").block();
        assertThat(updated).isNotNull();
        assertThat(updated.isActivated()).isTrue();
        assertThat(updated.getResetKey()).isNull();
        assertThat(updated.getResetDate()).isNull();
        assertThat(passwordEncoder.matches("StrongPass1!", updated.getPassword())).isTrue();

        userService.deleteUser("cpw-fin").block();
    }

    @Test
    void testFinishCreatePasswordWeakPassword_shouldFail() throws Exception {
        User user = new User();
        user.setLogin("cpw-weak"); // <= 20
        user.setEmail("cpw-weak@example.com");
        user.setLangKey("en");
        user.setCreatedBy(Constants.SYSTEM);
        user.setActivated(false);
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setResetKey("CPWKEYW");
        user.setResetDate(Instant.now().minusSeconds(60));
        userRepository.save(user).block();

        KeyAndPasswordVM body = new KeyAndPasswordVM();
        body.setKey("CPWKEYW");
        body.setNewPassword("weak"); // fails pattern inside service

        accountWebTestClient
            .post()
            .uri("/api/account/create-password/finish")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(body))
            .exchange()
            .expectStatus().isBadRequest();

        userService.deleteUser("cpw-weak").block();
    }

    @Test
    void testFinishCreatePasswordAlreadyActive_shouldFail() throws Exception {
        User user = new User();
        user.setLogin("cpw-act"); // <= 20
        user.setEmail("cpw-act@example.com");
        user.setLangKey("en");
        user.setCreatedBy(Constants.SYSTEM);
        user.setActivated(true); // active
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setResetKey("CPWKEYA");
        user.setResetDate(Instant.now().minusSeconds(60));
        userRepository.save(user).block();

        KeyAndPasswordVM body = new KeyAndPasswordVM();
        body.setKey("CPWKEYA");
        body.setNewPassword("StrongPass1!");

        accountWebTestClient
            .post()
            .uri("/api/account/create-password/finish")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(body))
            .exchange()
            .expectStatus().is4xxClientError();

        userService.deleteUser("cpw-act").block();
    }

}
