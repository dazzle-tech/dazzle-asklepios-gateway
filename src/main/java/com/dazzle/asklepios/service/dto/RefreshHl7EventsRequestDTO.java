package com.dazzle.asklepios.service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RefreshHl7EventsRequestDTO(
    @NotNull Long encounterId,
    @NotEmpty List<Long> orderTestIds
) {
}
