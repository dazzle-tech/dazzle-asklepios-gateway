package com.dazzle.asklepios.domain;

import com.dazzle.asklepios.config.Constants;
import com.dazzle.asklepios.domain.enumeration.Gender;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;


@EqualsAndHashCode(callSuper = true)
@Table("app_user")
@Data
public class User extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @NotNull
    @Pattern(regexp = Constants.LOGIN_REGEX)
    @Size(min = 1, max = 50)
    private String login;

    @JsonIgnore
    @NotNull
    @Size(min = 60, max = 60)
    @Column("password_hash")
    private String password;

    @Size(max = 50)
    @Column("first_name")
    private String firstName;

    @Size(max = 50)
    @Column("last_name")
    private String lastName;

    @Email
    @Size(min = 5, max = 254)
    private String email;

    @NotNull
    private boolean activated = false;

    @Size(min = 2, max = 10)
    @Column("lang_key")
    private String langKey;

    @Size(max = 256)
    @Column("image_url")
    private String imageUrl;

    @Size(max = 20)
    @Column("reset_key")
    @JsonIgnore
    private String resetKey;

    @Column("reset_date")
    private Instant resetDate = null;

    @Size(max = 20)
    @Column("phone_number")
    private String phoneNumber;

    @Column("birth_date")
    private java.time.LocalDate birthDate;


    @Enumerated(EnumType.STRING)
    private Gender gender;


    @JsonIgnore
    @org.springframework.data.annotation.Transient
    private Set<Authority> authorities = new HashSet<>();
}
