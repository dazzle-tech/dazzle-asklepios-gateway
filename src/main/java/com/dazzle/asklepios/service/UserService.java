package com.dazzle.asklepios.service;

import com.dazzle.asklepios.config.Constants;
import com.dazzle.asklepios.domain.Authority;
import com.dazzle.asklepios.domain.User;
import com.dazzle.asklepios.domain.enumeration.Gender;
import com.dazzle.asklepios.repository.AuthorityRepository;
import com.dazzle.asklepios.repository.UserRepository;
import com.dazzle.asklepios.security.SecurityUtils;
import com.dazzle.asklepios.service.dto.AdminUserDTO;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Set;

/**
 * Service class for managing users.
 */
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthorityRepository authorityRepository;
    private final MailService mailService;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthorityRepository authorityRepository, MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.mailService = mailService;
    }

    @Transactional
    public Mono<User> completePasswordReset(String newPassword, String key) {
        LOG.debug("Reset user password for reset key {}", key);
        return userRepository
            .findOneByResetKey(key)
            .filter(user -> user.getResetDate().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)))
            .publishOn(Schedulers.boundedElastic())
            .map(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetKey(null);
                user.setResetDate(null);
                return user;
            })
            .flatMap(this::saveUser);
    }

    @Transactional
    public Mono<User> requestPasswordReset(String mail) {
        return userRepository
            .findOneByEmailIgnoreCase(mail)
            .filter(User::isActivated)
            .publishOn(Schedulers.boundedElastic())
            .map(user -> {
                user.setResetKey(RandomUtil.generateResetKey());
                user.setResetDate(Instant.now());
                return user;
            })
            .flatMap(this::saveUser);
    }

    @Transactional
    public Mono<User> createUser(AdminUserDTO userDTO) {
        User user = new User();
        user.setLogin(userDTO.getLogin().toLowerCase());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail().toLowerCase());
        }
        user.setImageUrl(userDTO.getImageUrl());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setBirthDate(userDTO.getBirthDate());
        user.setGender(userDTO.getGender());
        user.setLangKey(userDTO.getLangKey() == null ? Constants.DEFAULT_LANGUAGE : userDTO.getLangKey());

        return Mono.fromCallable(() -> {

                String rawPassword = RandomUtil.generatePassword();
                user.setPassword(passwordEncoder.encode(rawPassword));
                user.setResetKey(RandomUtil.generateResetKey());
                user.setResetDate(Instant.now());
                user.setActivated(true);
                return new AbstractMap.SimpleEntry<>(user, rawPassword);
            })
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(entry -> saveUser(entry.getKey())
                .flatMap(savedUser -> sendWelcomeEmail(savedUser, entry.getValue()).thenReturn(savedUser))
            )
            .doOnNext(savedUser -> LOG.debug("Created user {} and sent password email", savedUser.getLogin()));
    }

    private Mono<Void> sendWelcomeEmail(User user, String rawPassword) {
        if (user.getEmail() == null) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> {
                mailService.sendNewUserPasswordMail(user, rawPassword);
            })
            .subscribeOn(Schedulers.boundedElastic())
            .then();
    }




    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update.
     * @return updated user.
     */
    @Transactional
    public Mono<AdminUserDTO> updateUser(AdminUserDTO userDTO) {
        return userRepository
            .findById(userDTO.getId())
            .map(user -> {
                user.setLogin(userDTO.getLogin().toLowerCase());
                user.setFirstName(userDTO.getFirstName());
                user.setLastName(userDTO.getLastName());
                if (userDTO.getEmail() != null) {
                    user.setEmail(userDTO.getEmail().toLowerCase());
                }
                user.setImageUrl(userDTO.getImageUrl());
                user.setActivated(userDTO.isActivated());
                user.setLangKey(userDTO.getLangKey());
                user.setPhoneNumber(userDTO.getPhoneNumber());
                user.setBirthDate(userDTO.getBirthDate());
                user.setGender(userDTO.getGender());
                Set<Authority> managedAuthorities = user.getAuthorities();
                managedAuthorities.clear();
                return user;
            })
            .flatMap(this::saveUser)
            .doOnNext(user -> LOG.debug("Update Information for User by admin: {}", user))
            .map(AdminUserDTO::new);
    }

    @Transactional
    public Mono<Void> deleteUser(String login) {
        return userRepository
            .findOneByLogin(login)
            .flatMap(user -> userRepository.delete(user).thenReturn(user))
            .doOnNext(user -> LOG.debug("Deleted User: {}", user))
            .then();
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     *
     * @param firstName first name of user.
     * @param lastName  last name of user.
     * @param email     email id of user.
     * @param langKey   language key.
     * @param imageUrl  image URL of user.
     * @return a completed {@link Mono}.
     */
    @Transactional
    public Mono<Void> updateUser(
        String firstName,
        String lastName,
        String email,
        String langKey,
        String imageUrl,
        String phoneNumber,
        java.time.LocalDate birthDate,
        Gender gender

    ) {
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .flatMap(user -> {
                user.setFirstName(firstName);
                user.setLastName(lastName);
                if (email != null) {
                    user.setEmail(email.toLowerCase());
                }
                user.setLangKey(langKey);
                user.setImageUrl(imageUrl);

                user.setPhoneNumber(phoneNumber);
                user.setBirthDate(birthDate);
                user.setGender(gender);

                return saveUser(user);
            })
            .doOnNext(user -> LOG.debug("Update Information for User by user: {}", user))
            .then();
    }

    @Transactional
    public Mono<User> saveUser(User user) {
        return SecurityUtils.getCurrentUserLogin()
            .switchIfEmpty(Mono.just(Constants.SYSTEM))
            .flatMap(login -> {
                if (user.getCreatedBy() == null) {
                    user.setCreatedBy(login);
                }
                user.setLastModifiedBy(login);

                return userRepository
                    .save(user);
            });
    }

    @Transactional
    public Mono<Void> changePassword(String currentClearTextPassword, String newPassword) {
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .publishOn(Schedulers.boundedElastic())
            .map(user -> {
                String currentEncryptedPassword = user.getPassword();
                if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                    throw new InvalidPasswordException();
                }
                String encryptedPassword = passwordEncoder.encode(newPassword);
                user.setPassword(encryptedPassword);
                return user;
            })
            .flatMap(this::saveUser)
            .doOnNext(user -> LOG.debug("Changed password for User: {}", user))
            .then();
    }

    @Transactional(readOnly = true)
    public Flux<AdminUserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAllWithAuthorities(pageable).map(AdminUserDTO::new);
    }

    @Transactional(readOnly = true)
    public Mono<Long> countManagedUsers() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public Mono<User> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneWithAuthoritiesByLogin(login);
    }

    @Transactional(readOnly = true)
    public Mono<User> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(user -> SecurityUtils.getCurrentUserFacility()
                .flatMap(facilityId ->
                    userRepository.findOneWithAuthoritiesByLoginAndFacilityId(user, facilityId)
                        .switchIfEmpty(Mono.error(new NotValidTokenException("User could not be found for the provided login and facility"))))
            )
            .switchIfEmpty(Mono.error(new NotValidTokenException("User login or facility not found")));
    }

    /**
     * Gets a list of all the authorities.
     * @return a list of all the authorities.
     */
    @Transactional(readOnly = true)
    public Flux<String> getAuthorities() {
        return authorityRepository.findAll().map(Authority::getName);
    }

    public final class RandomUtil {
        private static final int DEF_COUNT = 20;
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();

        private RandomUtil() {
        }

        public static String generateRandomAlphanumericString() {
            return RandomStringUtils.random(20, 0, 0, true, true, null, SECURE_RANDOM);
        }

        public static String generatePassword() {
            return generateRandomAlphanumericString();
        }

        public static String generateResetKey() {
            return generateRandomAlphanumericString();
        }

        static {
            SECURE_RANDOM.nextBytes(new byte[64]);
        }
    }
}
