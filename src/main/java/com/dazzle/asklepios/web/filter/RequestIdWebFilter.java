package com.dazzle.asklepios.web.filter;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Assigns a request id at the edge: it is forwarded to the downstream services in the {@code X-Request-Id}
 * header, echoed back to the client and kept as an exchange attribute for gateway logs and error responses.
 * Runs before Spring Security so that rejected requests get an id too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdWebFilter implements WebFilter {

    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    // The header comes from the client, so only accept short, log-safe values.
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        String requestId = incoming != null && VALID_REQUEST_ID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> headers.set(REQUEST_ID_HEADER, requestId)).build();
        ServerWebExchange mutated = exchange.mutate().request(request).build();

        mutated.getAttributes().put(REQUEST_ID, requestId);
        mutated.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);

        return chain.filter(mutated);
    }

    public static String getRequestId(ServerWebExchange exchange) {
        return exchange.getAttribute(REQUEST_ID);
    }
}
