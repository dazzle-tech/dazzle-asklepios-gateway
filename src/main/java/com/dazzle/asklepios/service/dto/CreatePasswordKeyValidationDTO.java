package com.dazzle.asklepios.service.dto;

public record CreatePasswordKeyValidationDTO(
    boolean valid,
    boolean activated,
    boolean passwordAlreadySet,
    String message
) {}

