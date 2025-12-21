package com.dazzle.asklepios.service;

import com.dazzle.asklepios.IntegrationTest;
import com.dazzle.asklepios.config.Constants;
import com.dazzle.asklepios.domain.User;
import com.dazzle.asklepios.repository.UserRepository;
import com.dazzle.asklepios.service.dto.CreatePasswordKeyValidationDTO;
import com.dazzle.asklepios.service.errors.UserAlreadyActiveException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests for {@link UserService} – create password flow.
 */
@IntegrationTest
class UserServiceCreatePasswordIT {

    private static final String DEFAULT_LOGIN = "johndoe_createpw";
    private static final String DEFAULT_EMAIL = "johndoe_createpw@localhost";
    private static final String DEFAULT_FIRSTNAME = "john";
    private static final String DEFAULT_LASTNAME = "doe";
    private static final String DEFAULT_LANGKEY = "en";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DatabaseClient databaseClient;

    private User user;

    @BeforeEach
    void init() {
        user = new User();
        user.setLogin(DEFAULT_LOGIN);
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60)); // placeholder
        user.setActivated(false);
        user.setEmail(DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setCreatedBy(Constants.SYSTEM);

        user.setResetKey(UserService.RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
    }

    @AfterEach
    void cleanup() {
        databaseClient
            .sql("DELETE FROM user_role WHERE user_id IN (SELECT id FROM app_user)")
            .fetch()
            .rowsUpdated()
            .block();

        userRepository.deleteAll().block();
    }

    @Test
    void validateCreatePasswordKey_shouldReturnTokenNotFound() {
        CreatePasswordKeyValidationDTO dto =
            userService.validateCreatePasswordKey("missing").block();

        assertThat(dto).isNotNull();
        assertThat(dto.valid()).isFalse();
        assertThat(dto.message()).isEqualTo("TOKEN_NOT_FOUND");
    }

    @Test
    void validateCreatePasswordKey_shouldReturnOk_whenValid() {
        userRepository.save(user).block();

        CreatePasswordKeyValidationDTO dto =
            userService.validateCreatePasswordKey(user.getResetKey()).block();

        assertThat(dto.valid()).isTrue();
        assertThat(dto.activated()).isFalse();
        assertThat(dto.message()).isEqualTo("OK");
    }

    @Test
    void validateCreatePasswordKey_shouldReturnExpired_whenTokenExpired() {
        user.setResetDate(Instant.now().minus(25, ChronoUnit.HOURS));
        userRepository.save(user).block();

        CreatePasswordKeyValidationDTO dto =
            userService.validateCreatePasswordKey(user.getResetKey()).block();

        assertThat(dto.valid()).isFalse();
        assertThat(dto.message()).isEqualTo("TOKEN_INVALID_OR_EXPIRED");
    }

    @Test
    void validateCreatePasswordKey_shouldReturnAlreadyActive_whenActivated() {
        user.setActivated(true);
        userRepository.save(user).block();

        CreatePasswordKeyValidationDTO dto =
            userService.validateCreatePasswordKey(user.getResetKey()).block();

        assertThat(dto.valid()).isFalse();
        assertThat(dto.activated()).isTrue();
        assertThat(dto.message()).isEqualTo("USER_ALREADY_ACTIVE");
    }

    @Test
    void completeCreatePassword_shouldActivateUser_andClearToken() {
        userRepository.save(user).block();

        String strongPassword = "MyNewPass123!";

        Optional<User> maybe =
            userService.completeCreatePassword(strongPassword, user.getResetKey()).blockOptional();

        assertThat(maybe).isPresent();

        User saved = userRepository.findOneByLogin(DEFAULT_LOGIN).block();
        assertThat(saved).isNotNull();
        assertThat(saved.isActivated()).isTrue();
        assertThat(saved.getResetKey()).isNull();
        assertThat(saved.getResetDate()).isNull();
        assertThat(saved.getPassword()).isNotEqualTo(user.getPassword());
    }

    @Test
    void completeCreatePassword_shouldFail_whenPasswordWeak() {
        user.setActivated(false);
        user.setResetKey(UserService.RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        userRepository.save(user).block();

        try {
            userService.completeCreatePassword("12345678", user.getResetKey()).block();
            fail("Expected InvalidPasswordException");
        } catch (Exception ex) {
            assertThat(root(ex)).isInstanceOf(InvalidPasswordException.class);
        }
    }

    @Test
    void completeCreatePassword_shouldFail_whenTokenExpired() {
        user.setResetDate(Instant.now().minus(25, ChronoUnit.HOURS));
        userRepository.save(user).block();

        Optional<User> maybe =
            userService.completeCreatePassword("MyNewPass123!", user.getResetKey()).blockOptional();

        assertThat(maybe).isNotPresent();
    }

    @Test
    void completeCreatePassword_shouldFail_whenUserAlreadyActive() {
        user.setActivated(true);
        user.setResetKey(UserService.RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        userRepository.save(user).block();

        try {
            userService.completeCreatePassword("MyNewPass123!", user.getResetKey()).block();
            fail("Expected UserAlreadyActiveException");
        } catch (Exception ex) {
            assertThat(root(ex)).isInstanceOf(UserAlreadyActiveException.class);
        }
    }


    private static Throwable root(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }
}
