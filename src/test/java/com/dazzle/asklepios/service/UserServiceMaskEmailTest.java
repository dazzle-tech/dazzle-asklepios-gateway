package com.dazzle.asklepios.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserServiceMaskEmailTest {

    @Test
    void keepsFirstTwoCharsAndDomain() {
        assertThat(UserService.maskEmail("hanan@gmail.com")).isEqualTo("ha****@gmail.com");
    }

    @Test
    void shortLocalPartKeepsOnlyFirstChar() {
        assertThat(UserService.maskEmail("ab@x.com")).isEqualTo("a****@x.com");
        assertThat(UserService.maskEmail("a@x.com")).isEqualTo("a****@x.com");
    }

    @Test
    void malformedEmailIsFullyHidden() {
        assertThat(UserService.maskEmail("no-at-sign")).isEqualTo("****");
        assertThat(UserService.maskEmail("@x.com")).isEqualTo("****");
    }
}
