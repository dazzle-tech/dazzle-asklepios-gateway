package com.dazzle.asklepios.domain;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthorityAsserts {


    /**
     * Asserts that the entity has all the updatable fields set.
     *
     * @param expected the expected entity
     * @param actual the actual entity
     */
    public static void assertAuthorityUpdatableFieldsEquals(Authority expected, Authority actual) {
        assertThat(actual)
            .as("Verify Authority relevant properties")
            .satisfies(a -> assertThat(a.getName()).as("check name").isEqualTo(expected.getName()));
    }


}
