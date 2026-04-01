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
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    Flux<Tuple2<Long, Optional<String>>> findAuthoritiesByUserIds(List<Long> userIds);
}

@Slf4j
class UserRepositoryInternalImpl implements UserRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final R2dbcConverter r2dbcConverter;

    public UserRepositoryInternalImpl(
        DatabaseClient db,
        R2dbcEntityTemplate r2dbcEntityTemplate,
        R2dbcConverter r2dbcConverter
    ) {
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
            .then(
                r2dbcEntityTemplate
                    .delete(User.class)
                    .matching(query(where("id").is(user.getId())))
                    .all()
                    .then()
            );
    }

    // ======= FIXED METHODS (no manual SQL) =======
    @Override
    public Flux<Tuple2<Long, Optional<String>>> findAuthoritiesByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Flux.empty();
        }

        String placeholders = IntStream.range(0, userIds.size())
            .mapToObj(i -> ":id" + i)
            .collect(Collectors.joining(", "));

        String sql = """
        SELECT
            ur.user_id,
            ra.authority_name
        FROM user_role ur
        LEFT JOIN role_authority ra ON ur.role_id = ra.role_id
        WHERE ur.user_id IN (%s)
        """.formatted(placeholders);

        DatabaseClient.GenericExecuteSpec spec = db.sql(sql);

        for (int i = 0; i < userIds.size(); i++) {
            spec = spec.bind("id" + i, userIds.get(i));
        }

        return spec
            .map((row, metadata) ->
                Tuples.of(
                    row.get("user_id", Long.class),
                    Optional.ofNullable(row.get("authority_name", String.class))
                )
            )
            .all();
    }

    @Override
    public Flux<User> findBasicUsers(String login, String email, String name, Pageable pageable) {
        StringBuilder sql = new StringBuilder("""
        SELECT
            u.id,
            u.login,
            u.first_name,
            u.last_name,
            u.email,
            u.image_url,
            u.activated,
            u.lang_key,
            u.created_by,
            u.created_date,
            u.last_modified_by,
            u.last_modified_date,
            u.phone_number,
            u.birth_date,
            u.gender,
            u.job_role
        FROM app_user u
        WHERE 1 = 1
        """);

        Map<String, Object> params = new HashMap<>();

        if (login != null && !login.isBlank()) {
            sql.append(" AND LOWER(u.login) LIKE LOWER(:login) ");
            params.put("login", "%" + login + "%");
        }

        if (email != null && !email.isBlank()) {
            sql.append(" AND LOWER(u.email) LIKE LOWER(:email) ");
            params.put("email", "%" + email + "%");
        }

        if (name != null && !name.isBlank()) {
            sql.append("""
            AND (
                LOWER(u.first_name) LIKE LOWER(:name)
                OR LOWER(u.last_name) LIKE LOWER(:name)
            )
            """);
            params.put("name", "%" + name + "%");
        }

        String property = pageable.getSort().stream()
            .map(Sort.Order::getProperty)
            .findFirst()
            .orElse("id");

        String direction = pageable.getSort().stream()
            .map(order -> order.getDirection().name())
            .findFirst()
            .orElse("ASC");

        Set<String> allowedSortFields = Set.of(
            "id",
            "login",
            "first_name",
            "last_name",
            "email",
            "created_date",
            "last_modified_date"
        );

        if (!allowedSortFields.contains(property)) {
            property = "id";
        }

        sql.append(" ORDER BY u.").append(property).append(" ").append(direction);
        sql.append(" LIMIT :limit OFFSET :offset");

        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        DatabaseClient.GenericExecuteSpec spec = db.sql(sql.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            spec = spec.bind(entry.getKey(), entry.getValue());
        }

        return spec
            .map((row, metadata) -> r2dbcConverter.read(User.class, row, metadata))
            .all()
            .collectList()
            .flatMapMany(users -> {
                if (users.isEmpty()) {
                    return Flux.empty();
                }

                List<Long> userIds = users.stream()
                    .map(User::getId)
                    .toList();

                return findAuthoritiesByUserIds(userIds)
                    .collectMultimap(
                        Tuple2::getT1,
                        tuple -> tuple.getT2().orElse(null)
                    )
                    .flatMapMany(authoritiesMap -> {
                        users.forEach(user -> {
                            @SuppressWarnings("unchecked")
                            Collection<String> authorityNames =
                                (Collection<String>) authoritiesMap.get(user.getId());

                            if (authorityNames == null || authorityNames.isEmpty()) {
                                user.setAuthorities(new HashSet<>());
                                return;
                            }

                            user.setAuthorities(
                                authorityNames.stream()
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .map(authorityName -> {
                                        Authority authority = new Authority();
                                        authority.setName(authorityName);
                                        return authority;
                                    })
                                    .collect(Collectors.toSet())
                            );
                        });

                        return Flux.fromIterable(users);
                    });
            });
    }
    @Override
    public Mono<Long> countBasicUsers(String login, String email, String name) {

        Criteria criteria = Criteria.empty();

        if (login != null && !login.isBlank()) {
            criteria = criteria.and(
                where("login").like("%" + login + "%").ignoreCase(true)
            );
        }

        if (email != null && !email.isBlank()) {
            criteria = criteria.and(
                where("email").like("%" + email + "%").ignoreCase(true)
            );
        }

        if (name != null && !name.isBlank()) {
            Criteria firstName = where("first_name").like("%" + name + "%").ignoreCase(true);
            Criteria lastName  = where("last_name").like("%" + name + "%").ignoreCase(true);
            criteria = criteria.and(firstName.or(lastName));
        }

        org.springframework.data.relational.core.query.Query q =
            org.springframework.data.relational.core.query.Query.query(criteria);

        return r2dbcEntityTemplate.count(q, User.class);
    }

    // ===========================================

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
