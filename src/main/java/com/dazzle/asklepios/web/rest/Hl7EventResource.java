package com.dazzle.asklepios.web.rest;
import com.dazzle.asklepios.service.Hl7EventService;
import com.dazzle.asklepios.service.dto.RefreshHl7EventsRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/hl7-events")
@RequiredArgsConstructor
public class Hl7EventResource {

    private final Hl7EventService hl7EventService;

    @PostMapping("/refresh")
    public Mono<ResponseEntity<Void>> refreshEvents(
        @Valid @RequestBody RefreshHl7EventsRequestDTO request
    ) {
        return hl7EventService
            .refreshEvents(request)
            .thenReturn(ResponseEntity.noContent().build());
    }
}
