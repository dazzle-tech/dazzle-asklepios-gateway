package com.dazzle.asklepios.domain;

import java.util.UUID;

public class AuthorityTestSamples {

    public static Authority getAuthoritySample1() {
        Authority authority = new Authority();
        authority.setName("name1");
        return authority;
    }

    public static Authority getAuthoritySample2() {
        Authority authority = new Authority();
        authority.setName("name2");
        return authority;
    }

    public static Authority getAuthorityRandomSampleGenerator() {
        Authority authority = new Authority();
        authority.setName(UUID.randomUUID().toString());
        return authority;
    }
}
