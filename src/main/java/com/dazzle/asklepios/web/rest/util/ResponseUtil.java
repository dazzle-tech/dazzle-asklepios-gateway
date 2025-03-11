package com.dazzle.asklepios.web.rest.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

public interface ResponseUtil {
    static <X> Mono<ResponseEntity<X>> wrapOrNotFound(Mono<X> maybeResponse) {
        return wrapOrNotFound(maybeResponse, (HttpHeaders)null);
    }

    static <X> Mono<ResponseEntity<X>> wrapOrNotFound(Mono<X> maybeResponse, HttpHeaders headers) {
        return maybeResponse.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND))).map((response) -> ((ResponseEntity.BodyBuilder)ResponseEntity.ok().headers(headers)).body(response));
    }
}
