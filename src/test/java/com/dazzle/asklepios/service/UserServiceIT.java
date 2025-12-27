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
 * Integration tests for {@link UserService}.
 */
@IntegrationTest
class UserServiceIT {

    private static final String DEFAULT_LOGIN = "johndoe_service";
    private static final String DEFAULT_EMAIL = "johndoe_service@localhost";
    private static final String DEFAULT_FIRSTNAME = "john";
    private static final String DEFAULT_LASTNAME = "doe";
    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final String DEFAULT_LANGKEY = "en";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DatabaseClient databaseClient;

    private User user;

    @BeforeEach
    public void init() {
        user = new User();
        user.setLogin(DEFAULT_LOGIN);
        user.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        user.setActivated(true);
        user.setEmail(DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setCreatedBy(Constants.SYSTEM);
    }

    @AfterEach
    public void cleanupAndCheck() {
        String deleteUserRole = "DELETE FROM user_role WHERE user_id IN (SELECT id FROM app_user)";
        databaseClient.sql(deleteUserRole)
            .fetch()
            .rowsUpdated()
            .block();
        userRepository.deleteAll().block();
    }

    // ------------------------
    // Reset password tests
    // ------------------------

    @Test
    void assertThatUserMustExistToResetPassword() {
        userRepository.save(user).block();

        Optional<User> maybeUser = userService.requestPasswordReset("invalid.login@localhost").blockOptional();
        assertThat(maybeUser).isNotPresent();

        maybeUser = userService.requestPasswordReset(user.getEmail()).blockOptional();
        assertThat(maybeUser).isPresent();
        assertThat(maybeUser.orElseThrow().getEmail()).isEqualTo(user.getEmail());
        assertThat(maybeUser.orElseThrow().getResetDate()).isNotNull();
        assertThat(maybeUser.orElseThrow().getResetKey()).isNotNull();
    }

    @Test
    void assertThatOnlyActivatedUserCanRequestPasswordReset() {
        user.setActivated(false);
        userRepository.save(user).block();

        Optional<User> maybeUser = userService.requestPasswordReset(user.getLogin()).blockOptional();
        assertThat(maybeUser).isNotPresent();
    }

    @Test
    void assertThatResetKeyMustNotBeOlderThan24Hours() {
        Instant daysAgo = Instant.now().minus(25, ChronoUnit.HOURS);
        String resetKey = UserService.RandomUtil.generateResetKey();

        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey(resetKey);
        userRepository.save(user).block();

        Optional<User> maybeUser = userService.completePasswordReset("JohnDoe123!", user.getResetKey()).blockOptional();
        assertThat(maybeUser).isNotPresent();
    }

    @Test
    void assertThatResetKeyMustBeValid() {
        Instant daysAgo = Instant.now().minus(25, ChronoUnit.HOURS);
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey("1234");
        userRepository.save(user).block();

        Optional<User> maybeUser = userService.completePasswordReset("JohnDoe123!", user.getResetKey()).blockOptional();
        assertThat(maybeUser).isNotPresent();
    }

    @Test
    void assertThatUserCanResetPassword() {
        String oldPassword = user.getPassword();
        Instant hoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);
        String resetKey = UserService.RandomUtil.generateResetKey();

        user.setActivated(true);
        user.setResetDate(hoursAgo);
        user.setResetKey(resetKey);
        userRepository.save(user).block();

        Optional<User> maybeUser = userService.completePasswordReset("JohnDoe123!", user.getResetKey()).blockOptional();
        assertThat(maybeUser).isPresent();

        User saved = userRepository.findOneByLogin(DEFAULT_LOGIN).block();
        assertThat(saved).isNotNull();
        assertThat(saved.getResetDate()).isNull();
        assertThat(saved.getResetKey()).isNull();
        assertThat(saved.getPassword()).isNotEqualTo(oldPassword);
    }

    @Test
    void assertThatResetPasswordRejectsWeakPasswordPattern() {
        Instant hoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);
        user.setActivated(true);
        user.setResetDate(hoursAgo);
        user.setResetKey(UserService.RandomUtil.generateResetKey());
        userRepository.save(user).block();

        try {
            userService.completePasswordReset("12345678", user.getResetKey()).block();
            fail("Expected InvalidPasswordException");
        } catch (Exception ex) {
            assertThat(root(ex)).isInstanceOf(InvalidPasswordException.class);
        }
    }

    private static Throwable root(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    // ------------------------
    // Create password tests
    // ------------------------

    @Test
    void validateCreatePasswordKey_shouldReturnTokenNotFound() {
        CreatePasswordKeyValidationDTO dto = userService.validateCreatePasswordKey("missing").block();

        assertThat(dto).isNotNull();
        assertThat(dto.valid()).isFalse();
        assertThat(dto.message()).isEqualTo("TOKEN_NOT_FOUND");
        assertThat(dto.activated()).isFalse();
    }

    @Test
    void validateCreatePasswordKey_shouldReturnOk_whenValidAndNotActivated() {
        user.setActivated(false);
        user.setResetKey(UserService.RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        userRepository.save(user).block();

        CreatePasswordKeyValidationDTO dto = userService.validateCreatePasswordKey(user.getResetKey()).block();

        assertThat(dto).isNotNull();
        assertThat(dto.valid()).isTrue();
        assertThat(dto.activated()).isFalse();
        assertThat(dto.message()).isEqualTo("OK");
    }

    @Test
    void validateCreatePasswordKey_shouldReturnExpired_whenTokenExpired() {
        user.setActivated(false);
        user.setResetKey(UserService.RandomUtil.generateResetKey());
        user.setResetDate(Instant.now().minus(25, ChronoUnit.HOURS));
        userRepository.save(user).block();

        CreatePasswordKeyValidationDTO dto = userService.validateCreatePasswordKey(user.getResetKey()).block();

        assertThat(dto).isNotNull();
        assertThat(dto.valid()).isFalse();
        assertThat(dto.message()).isEqualTo("TOKEN_INVALID_OR_EXPIRED");
    }

    @Test
    void validateCreatePasswordKey_shouldReturnAlreadyActive_whenActivated() {
        user.setActivated(true);
        user.setResetKey(UserService.RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        userRepository.save(user).block();

        CreatePasswordKeyValidationDTO dto = userService.validateCreatePasswordKey(user.getResetKey()).block();

        assertThat(dto).isNotNull();
        assertThat(dto.valid()).isFalse();
        assertThat(dto.activated()).isTrue();
        assertThat(dto.message()).isEqualTo("USER_ALREADY_ACTIVE");
    }

    @Test
    void completeCreatePassword_shouldActivateUser_andClearToken() {
        user.setActivated(false);
        user.setResetKey(UserService.RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        userRepository.save(user).block();

        String strongPassword = "MyNewPass123!";

        Optional<User> maybe = userService.completeCreatePassword(strongPassword, user.getResetKey()).blockOptional();
        assertThat(maybe).isPresent();

        User saved = userRepository.findOneByLogin(DEFAULT_LOGIN).block();
        assertThat(saved).isNotNull();
        assertThat(saved.isActivated()).isTrue();
        assertThat(saved.getResetKey()).isNull();
        assertThat(saved.getResetDate()).isNull();
    }

    @Test
    void completeCreatePassword_shouldFail_whenPasswordWeak() {
        user.setActivated(false); // MUST be false for this test
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
        user.setActivated(false);
        user.setResetKey(UserService.RandomUtil.generateResetKey());
        user.setResetDate(Instant.now().minus(25, ChronoUnit.HOURS));
        userRepository.save(user).block();

        Optional<User> maybe = userService.completeCreatePassword("MyNewPass123!", user.getResetKey()).blockOptional();
        assertThat(maybe).isNotPresent();
    }


    @Test
    void completeCreatePassword_shouldFail_whenUserAlreadyActive() {
        user.setActivated(true); // explicitly true
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

}
