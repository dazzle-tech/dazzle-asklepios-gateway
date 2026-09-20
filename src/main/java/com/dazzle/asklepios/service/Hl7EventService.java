package com.dazzle.asklepios.service;

import com.dazzle.asklepios.service.dto.RefreshHl7EventsRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Hl7EventService {

    private final DatabaseClient db;

    public Mono<Void> refreshEvents(
        RefreshHl7EventsRequestDTO request
    ) {

        Mono<Void> encounterEvent =
            db.sql("""
                    INSERT INTO "HL7_INTEGRATION".hl7_event
                    (
                        id,
                        event_type,
                        object_key,
                        event_datetime
                    )
                    VALUES
                    (
                        gen_random_uuid(),
                        'PATIENT_VISIT_CREATED',
                        :encounterId,
                        NOW()
                    )
                    """)
                .bind("encounterId", request.encounterId().toString())
                .then();

        List<Mono<Void>> orderEvents =
            request.orderTestIds()
                .stream()
                .map(orderTestId ->
                    db.sql("""
                            INSERT INTO "HL7_INTEGRATION".hl7_event
                            (
                                id,
                                event_type,
                                object_key,
                                event_datetime
                            )
                            VALUES
                            (
                                gen_random_uuid(),
                                'ORDER_CREATED',
                                :orderTestId,
                                NOW()
                            )
                            """)
                        .bind("orderTestId", orderTestId.toString())
                        .then()
                )
                .toList();

        return encounterEvent
            .thenMany(Flux.concat(orderEvents))
            .then();
    }
}
