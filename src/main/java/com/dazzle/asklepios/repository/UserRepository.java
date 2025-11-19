package com.dazzle.asklepios.repository;

import com.dazzle.asklepios.domain.Authority;
import com.dazzle.asklepios.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanComparator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

/**
 * Spring Data R2DBC repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, Long>, UserRepositoryInternal {

    Mono<User> findOneByResetKey(String resetKey);

    Mono<User> findOneByEmailIgnoreCase(String email);

    Mono<User> findOneByLogin(String login);

    Mono<Long> count();

    @Query("INSERT INTO user_role VALUES(:userId, :roleId)")
    Mono<Void> saveUserRole(Long userId, Long roleId);

    @Query("DELETE FROM user_role WHERE user_id = :userId AND role_id = :roleId")
    Mono<Void> deleteUserRole(Long userId, Long roleId);



}

interface DeleteExtended<T> {
    Mono<Void> delete(T user);
}

interface UserRepositoryInternal extends DeleteExtended<User> {
    Mono<User> findOneWithAuthoritiesByLogin(String login);

    Mono<User> findOneWithAuthoritiesByLoginAndFacilityId(String login, Long facilityId);

    Mono<User> findOneWithAuthoritiesByEmailIgnoreCaseAndFacilityId(String email, Long facilityId);

    Flux<User> findAllWithAuthorities(Pageable pageable);

    Flux<User> findBasicUsers(String login, String email, String name, Pageable pageable);

    Mono<Long> countBasicUsers(String login, String email, String name);
}

@Slf4j
class UserRepositoryInternalImpl implements UserRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final R2dbcConverter r2dbcConverter;

    public UserRepositoryInternalImpl(DatabaseClient db, R2dbcEntityTemplate r2dbcEntityTemplate, R2dbcConverter r2dbcConverter) {
        this.db = db;
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
        this.r2dbcConverter = r2dbcConverter;
    }

    @Override
    public Mono<User> findOneWithAuthoritiesByLogin(String login) {
        return findOneWithAuthoritiesBy("login", login);
    }

    @Override
    public Mono<User> findOneWithAuthoritiesByLoginAndFacilityId(String login, Long facilityId) {
        return findOneWithAuthoritiesBy("login", login, facilityId);
    }

    @Override
    public Mono<User> findOneWithAuthoritiesByEmailIgnoreCaseAndFacilityId(String email, Long facilityId) {
        return findOneWithAuthoritiesBy("email", email.toLowerCase(), facilityId);
    }

    @Override
    public Flux<User> findAllWithAuthorities(Pageable pageable) {
        String property = pageable.getSort().stream().map(Sort.Order::getProperty).findFirst().orElse("id");
        String direction = String.valueOf(
            pageable.getSort().stream().map(Sort.Order::getDirection).findFirst().orElse(Sort.DEFAULT_DIRECTION)
        );
        long page = pageable.getPageNumber();
        long size = pageable.getPageSize();

        return db
            .sql("""
        SELECT u.*, ra.authority_name
        FROM app_user u
        LEFT JOIN user_role ur ON u.id = ur.user_id
        LEFT JOIN role_authority ra ON ur.role_id = ra.role_id
    """)
            .map((row, metadata) ->
                Tuples.of(
                    r2dbcConverter.read(User.class, row, metadata),
                    Optional.ofNullable(row.get("authority_name", String.class))
                )
            )
            .all()
            .groupBy(t -> t.getT1().getLogin())
            .flatMap(l -> l.collectList().map(t -> updateUserWithAuthorities(t.get(0).getT1(), t)))
            .sort(
                Sort.Direction.fromString(direction) == Sort.DEFAULT_DIRECTION
                    ? new BeanComparator<>(property)
                    : new BeanComparator<>(property).reversed()
            )
            .skip(page * size)
            .take(size);

    }

    @Override
    public Mono<Void> delete(User user) {
        return db
            .sql("DELETE FROM user_role WHERE user_id = :userId")
            .bind("userId", user.getId())
            .then()
            .then(r2dbcEntityTemplate.delete(User.class).matching(query(where("id").is(user.getId()))).all().then());
    }


    @Override
    public Flux<User> findBasicUsers(String login, String email, String name, Pageable pageable) {

        StringBuilder sql = new StringBuilder("""
        SELECT *
        FROM app_user
        WHERE 1 = 1
    """);

        // Filtering
        if (login != null && !login.isBlank()) {
            sql.append(" AND LOWER(login) LIKE LOWER('%").append(login).append("%') ");
        }

        if (email != null && !email.isBlank()) {
            sql.append(" AND LOWER(email) LIKE LOWER('%").append(email).append("%') ");
        }

        if (name != null && !name.isBlank()) {
            sql.append(" AND (LOWER(first_name) LIKE LOWER('%").append(name).append("%') ")
                .append(" OR LOWER(last_name) LIKE LOWER('%").append(name).append("%')) ");
        }

        // Sorting
        if (!pageable.getSort().isEmpty()) {
            Sort.Order order = pageable.getSort().iterator().next();
            sql.append(" ORDER BY ").append(order.getProperty()).append(" ").append(order.getDirection());
        } else {
            sql.append(" ORDER BY id ASC");
        }

        // Pagination
        sql.append(" LIMIT ").append(pageable.getPageSize());
        sql.append(" OFFSET ").append(pageable.getOffset());

        return db.sql(sql.toString())
            .map((row, meta) -> r2dbcConverter.read(User.class, row, meta))
            .all();
    }
    @Override
    public Mono<Long> countBasicUsers(String login, String email, String name) {

        StringBuilder sql = new StringBuilder("""
        SELECT COUNT(*) AS cnt
        FROM app_user
        WHERE 1 = 1
    """);

        if (login != null && !login.isBlank()) {
            sql.append(" AND LOWER(login) LIKE LOWER('%").append(login).append("%') ");
        }

        if (email != null && !email.isBlank()) {
            sql.append(" AND LOWER(email) LIKE LOWER('%").append(email).append("%') ");
        }

        if (name != null && !name.isBlank()) {
            sql.append(" AND (LOWER(first_name) LIKE LOWER('%").append(name).append("%') ")
                .append(" OR LOWER(last_name) LIKE LOWER('%").append(name).append("%')) ");
        }

        return db.sql(sql.toString())
            .map((row, meta) -> row.get("cnt", Long.class))
            .one();
    }

    private Mono<User> findOneWithAuthoritiesBy(String fieldName, Object fieldValue) {
        return db
            .sql("""
            SELECT *
            FROM app_user u
            LEFT JOIN user_role ur ON u.id = ur.user_id
            LEFT JOIN role_authority ra ON ur.role_id = ra.role_id
            WHERE u.
        """ + fieldName + " = :" + fieldName)
            .bind(fieldName, fieldValue)
            .map((row, metadata) ->
                Tuples.of(
                    r2dbcConverter.read(User.class, row, metadata),
                    Optional.ofNullable(row.get("authority_name", String.class))
                )
            )
            .all()
            .collectList()
            .filter(l -> !l.isEmpty())
            .map(l -> updateUserWithAuthorities(l.get(0).getT1(), l));
    }

    public Mono<User> findOneWithAuthoritiesBy(String fieldName, Object fieldValue, Long facilityId) {
        return db
            .sql("""
            SELECT *
            FROM app_user u
            LEFT JOIN user_role ur ON u.id = ur.user_id
            LEFT JOIN role r ON ur.role_id = r.id
            LEFT JOIN role_authority ra ON ur.role_id = ra.role_id
            WHERE u.
        """ + fieldName + " = :" + fieldName + " AND r.facility_id = :facilityId")
            .bind(fieldName, fieldValue)
            .bind("facilityId", facilityId)
            .map((row, metadata) ->
                Tuples.of(
                    r2dbcConverter.read(User.class, row, metadata),
                    Optional.ofNullable(row.get("authority_name", String.class))
                )
            )
            .all()
            .collectList()
            .filter(l -> !l.isEmpty())
            .map(l -> updateUserWithAuthorities(l.get(0).getT1(), l));
    }

    private User updateUserWithAuthorities(User user, List<Tuple2<User, Optional<String>>> tuples) {
        user.setAuthorities(
            tuples
                .stream()
                .filter(t -> t.getT2().isPresent())
                .map(t -> {
                    Authority authority = new Authority();
                    authority.setName(t.getT2().orElseThrow());
                    return authority;
                })
                .collect(Collectors.toSet())
        );

        return user;
    }
}
