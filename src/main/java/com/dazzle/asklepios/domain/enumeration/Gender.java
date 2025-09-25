package com.dazzle.asklepios.domain.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender {
    MALE, FEMALE;

    // to
    @JsonValue
    public String toValue() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }

    @JsonCreator
    public static Gender fromValue(String value) {
        if (value == null) return null;
        return Gender.valueOf(value.toUpperCase());
    }
}
