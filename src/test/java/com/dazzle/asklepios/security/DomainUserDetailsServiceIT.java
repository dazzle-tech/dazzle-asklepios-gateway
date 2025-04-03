package com.dazzle.asklepios.security;

import com.dazzle.asklepios.IntegrationTest;
import com.dazzle.asklepios.config.Constants;
import com.dazzle.asklepios.domain.User;
import com.dazzle.asklepios.repository.UserRepository;
import com.dazzle.asklepios.service.UserService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integrations tests for {@link DomainUserDetailsService}.
 */
@IntegrationTest
class DomainUserDetailsServiceIT {

    private static final String USER_ONE_LOGIN = "test-user-one";
    private static final String USER_ONE_EMAIL = "test-user-one@localhost.com";
    private static final String USER_TWO_LOGIN = "test-user-two";
    private static final String USER_TWO_EMAIL = "test-user-two@localhost.com";


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;
    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    @Qualifier("userDetailsService")
    private DomainUserDetailsService domainUserDetailsService;

    public User getUserOne() {
        User userOne = new User();
        userOne.setLogin(USER_ONE_LOGIN);
        userOne.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        userOne.setActivated(true);
        userOne.setEmail(USER_ONE_EMAIL);
        userOne.setFirstName("userOne");
        userOne.setLastName("doe");
        userOne.setLangKey("en");
        userOne.setCreatedBy(Constants.SYSTEM);
        return userOne;
    }

    public User getUserTwo() {
        User userTwo = new User();
        userTwo.setLogin(USER_TWO_LOGIN);
        userTwo.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        userTwo.setActivated(false);
        userTwo.setEmail(USER_TWO_EMAIL);
        userTwo.setFirstName("userTwo");
        userTwo.setLastName("doe");
        userTwo.setLangKey("en");
        userTwo.setCreatedBy(Constants.SYSTEM);
        return userTwo;
    }

    @BeforeEach
    public void init() {
        userRepository.save(getUserOne()).block();
        userRepository.save(getUserTwo()).block();

        String insertFacility = "INSERT INTO facility (name, type) VALUES ($1, $2)";
        databaseClient.sql(insertFacility)
            .bind("$1", "Facility One")
            .bind("$2", "Type A")
            .fetch()
            .rowsUpdated()
            .block();

        String insertAuthority = "INSERT INTO authority (name) VALUES ($1)";
        databaseClient.sql(insertAuthority)
            .bind("$1", "ROLE_SUPER")
            .fetch()
            .rowsUpdated()
            .block();

        String insertRole = "INSERT INTO role (name, type, facility_id) VALUES ($1, $2, (SELECT id FROM facility WHERE name = $3))";
        databaseClient.sql(insertRole)
            .bind("$1", "Admin")
            .bind("$2", "DOC")
            .bind("$3", "Facility One")
            .fetch()
            .rowsUpdated()
            .block();

        String insertRoleAuthority = "INSERT INTO role_authority (role_id, authority_name) " +
            "VALUES ((SELECT id FROM role WHERE name = $1 AND facility_id = (SELECT id FROM facility WHERE name = $2)), $3)";
        databaseClient.sql(insertRoleAuthority)
            .bind("$1", "Admin")
            .bind("$2", "Facility One")
            .bind("$3", "ROLE_SUPER")
            .fetch()
            .rowsUpdated()
            .block();

        String insertUserRole = "INSERT INTO user_role (user_id, role_id) " +
            "VALUES ((SELECT id FROM app_user WHERE login = $1), (SELECT id FROM role WHERE name = $2 AND facility_id = (SELECT id FROM facility WHERE name = $3)))";
        databaseClient.sql(insertUserRole)
            .bind("$1", USER_ONE_LOGIN)
            .bind("$2", "Admin")
            .bind("$3", "Facility One")
            .fetch()
            .rowsUpdated()
            .block();
        String insertUserTowRole = "INSERT INTO user_role (user_id, role_id) " +
            "VALUES ((SELECT id FROM app_user WHERE login = $1), (SELECT id FROM role WHERE name = $2 AND facility_id = (SELECT id FROM facility WHERE name = $3)))";
        databaseClient.sql(insertUserRole)
            .bind("$1", USER_TWO_LOGIN)
            .bind("$2", "Admin")
            .bind("$3", "Facility One")
            .fetch()
            .rowsUpdated()
            .block();
    }

    @AfterEach
    public void cleanup() {
        userService.deleteUser(USER_ONE_LOGIN).block();
        userService.deleteUser(USER_TWO_LOGIN).block();

        // Clean up data inserted into the facility, role, and other related tables
        String deleteUserRole = "DELETE FROM user_role WHERE user_id = (SELECT id FROM app_user WHERE login = $1)";
        databaseClient.sql(deleteUserRole)
            .bind("$1", USER_ONE_LOGIN)
            .fetch()
            .rowsUpdated()
            .block();

        String deleteRoleAuthority = "DELETE FROM role_authority WHERE role_id = (SELECT id FROM role WHERE name = $1 AND facility_id = (SELECT id FROM facility WHERE name = $2))";
        databaseClient.sql(deleteRoleAuthority)
            .bind("$1", "Admin")
            .bind("$2", "Facility One")
            .fetch()
            .rowsUpdated()
            .block();

        String deleteRole = "DELETE FROM role WHERE name = $1 AND facility_id = (SELECT id FROM facility WHERE name = $2)";
        databaseClient.sql(deleteRole)
            .bind("$1", "Admin")
            .bind("$2", "Facility One")
            .fetch()
            .rowsUpdated()
            .block();

        String deleteFacility = "DELETE FROM facility WHERE name = $1";
        databaseClient.sql(deleteFacility)
            .bind("$1", "Facility One")
            .fetch()
            .rowsUpdated()
            .block();

        String deleteAuthority = "DELETE FROM authority WHERE name = $1";
        databaseClient.sql(deleteAuthority)
            .bind("$1", "ROLE_SUPER")
            .fetch()
            .rowsUpdated()
            .block();

    }

    @Test
    void assertThatUserCanBeFoundByLogin() {
        UserDetails userDetails = domainUserDetailsService.findByUsernameAndFacility(USER_ONE_LOGIN, getFacilityId()).block();
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_LOGIN);
    }
    @Test
    void assertThatUserCanBeFoundByLoginIgnoreCase() {
        UserDetails userDetails = domainUserDetailsService.findByUsernameAndFacility(USER_ONE_LOGIN.toUpperCase(Locale.ENGLISH), getFacilityId()).block();
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_LOGIN);
    }

    @Test
    void assertThatUserCanBeFoundByEmail() {
        UserDetails userDetails = domainUserDetailsService.findByUsernameAndFacility(USER_ONE_EMAIL, getFacilityId()).block();
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_LOGIN);
    }

    @Test
    void assertThatUserCanBeFoundByEmailIgnoreCase() {
        UserDetails userDetails = domainUserDetailsService.findByUsernameAndFacility(USER_ONE_EMAIL.toUpperCase(Locale.ENGLISH), getFacilityId()).block();
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_LOGIN);
    }

    @Test
    void assertThatEmailIsPrioritizedOverLogin() {
        UserDetails userDetails = domainUserDetailsService.findByUsernameAndFacility(USER_ONE_EMAIL, getFacilityId()).block();
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_ONE_LOGIN);
    }

    @Test
    void assertThatUserNotActivatedExceptionIsThrownForNotActivatedUsers() {
        assertThatExceptionOfType(UserNotActivatedException.class).isThrownBy(() ->
            domainUserDetailsService.findByUsernameAndFacility(USER_TWO_LOGIN, getFacilityId()).block()
        );
    }

    private Long getFacilityId() {
        String selectFacilityId = "SELECT id FROM facility WHERE name = $1";
        Integer facilityId = databaseClient.sql(selectFacilityId)
            .bind("$1", "Facility One")
            .map((row, rowMetadata) -> row.get("id", Integer.class))
            .one()
            .block();
        return Long.valueOf(facilityId);
    }

    @Test
    void assertThatUserCanBeFoundByLoginAdminFacility1() {
        UserDetails user = domainUserDetailsService.findByUsernameAndFacility("admin", 1L).block();
        assertUserHasAuthorities(user, Set.of("ROLE_ADMIN", "ROLE_USER"));
    }

    @Test
    void assertThatUserCanBeFoundByLoginAdminFacility2() {
        UserDetails user = domainUserDetailsService.findByUsernameAndFacility("admin", 2L).block();
        assertUserHasAuthorities(user, Set.of("ROLE_ENCOUNTER"));
    }

    @Test
    void assertThatUserCanBeFoundByLoginUserFacility2() {
        UserDetails user = domainUserDetailsService.findByUsernameAndFacility("user", 2L).block();
        assertUserHasAuthorities(user, Set.of("ROLE_ENCOUNTER"));
    }

    @Test
    void assertThatUserCanBeFoundByLoginLabFacility2() {
        UserDetails user = domainUserDetailsService.findByUsernameAndFacility("lab", 1L).block();
        assertUserHasAuthorities(user, Set.of("ROLE_LAB"));
    }

    @Test
    void assertThatUserCanBeFoundByLoginAppointmentFacility3() {
        UserDetails user = domainUserDetailsService.findByUsernameAndFacility("appointment", 3L).block();
        assertUserHasAuthorities(user, Set.of("ROLE_APPOINTMENT"));
    }

    private void assertUserHasAuthorities(UserDetails user, Set<String> expectedAuthorities) {
        assertThat(user).isNotNull();
        Set<String> actualAuthorities = user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        assertThat(actualAuthorities).containsExactlyInAnyOrderElementsOf(expectedAuthorities);
    }
}
