package com.dazzle.asklepios.web.filter.requesttrace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.Optional;
import org.springframework.cloud.gateway.route.Route;

/**
 * Tracks state-changing gateway requests for troubleshooting and auditability.
 */
@Component
public class RequestTraceFilter implements GlobalFilter, Ordered {

    private static final Logger TRACE_LOG = LoggerFactory.getLogger("com.dazzle.asklepios.requesttrace");

    private static final long MAX_CAPTURE_BYTES = 1024L * 1024L;

    private static final Set<HttpMethod> TRACKED_METHODS = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);

    private final ObjectMapper objectMapper;
    private final RequestTracePayloadSanitizer payloadSanitizer;

    public RequestTraceFilter(ObjectMapper objectMapper, RequestTracePayloadSanitizer payloadSanitizer) {
        this.objectMapper = objectMapper;
        this.payloadSanitizer = payloadSanitizer;
        System.out.println(
            "######## REQUEST TRACE FILTER ACTIVE ########"
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!shouldTrace(request) || isMultipartRequest(request)) {
            return chain.filter(exchange);
        }

        long startedAt = System.nanoTime();
        return currentUser()
            .flatMap(username -> traceRequest(exchange, chain, username, startedAt));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private Mono<Void> traceRequest(
        ServerWebExchange exchange,
        GatewayFilterChain chain,
        String username,
        long startedAt
    ) {

        ServerHttpRequest request =
            exchange.getRequest();

        long contentLength =
            request.getHeaders().getContentLength();

        /*
         * Skip capturing payload if request body
         * exceeds configured maximum size.
         */
        if (
            contentLength > MAX_CAPTURE_BYTES
                && contentLength != -1
        ) {

            return chain.filter(exchange)
                .doOnSuccess(v ->
                    logTrace(
                        exchange,
                        username,
                        null,
                        null,
                        startedAt
                    )
                )
                .doOnError(ex ->
                    logTrace(
                        exchange,
                        username,
                        null,
                        ex,
                        startedAt
                    )
                );
        }

        return capturePayload(
            exchange,
            chain,
            username,
            startedAt
        );
    }
    private boolean shouldTrace(ServerHttpRequest request) {
        return request.getMethod() != null && TRACKED_METHODS.contains(request.getMethod());
    }

    private boolean isMultipartRequest(ServerHttpRequest request) {
        MediaType contentType = request.getHeaders().getContentType();
        return contentType != null && "multipart".equalsIgnoreCase(contentType.getType());
    }

    private Mono<String> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(context -> Mono.justOrEmpty(context.getAuthentication()))
            .filter(auth -> auth.isAuthenticated())
            .map(Authentication::getName)
            .defaultIfEmpty("anonymous");
    }

    private ServerHttpRequestDecorator decorateRequest(ServerWebExchange exchange, byte[] bodyBytes) {
        ServerHttpRequest request = exchange.getRequest();
        return new ServerHttpRequestDecorator(request) {
            @Override
            public reactor.core.publisher.Flux<DataBuffer> getBody() {
                if (bodyBytes.length == 0) {
                    return reactor.core.publisher.Flux.empty();
                }
                return reactor.core.publisher.Flux.defer(() -> reactor.core.publisher.Flux.just(exchange.getResponse().bufferFactory().wrap(bodyBytes)));
            }
        };
    }

    private byte[] bufferToBytes(DataBuffer buffer) {
        try {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            return bytes;
        } finally {
            DataBufferUtils.release(buffer);
        }
    }

    private JsonNode resolvePayload(MediaType contentType, byte[] bodyBytes) {
        if (bodyBytes.length == 0) {
            return null;
        }

        if (bodyBytes.length > MAX_CAPTURE_BYTES) {
            return null;
        }

        if (contentType != null && !isJson(contentType)) {
            return TextNode.valueOf(new String(bodyBytes, StandardCharsets.UTF_8));
        }

        try {
            return payloadSanitizer.sanitize(objectMapper.readTree(bodyBytes));
        } catch (IOException ex) {
            return TextNode.valueOf(new String(bodyBytes, StandardCharsets.UTF_8));
        }
    }

    private boolean isJson(MediaType mediaType) {
        String subtype = mediaType.getSubtype();
        return subtype != null && (subtype.contains("json") || subtype.endsWith("+json"));
    }

    private void logTrace(ServerWebExchange exchange, String username, JsonNode payload, Throwable error, long startedAt) {
        int status = determineStatus(exchange, error);
            RequestTraceLogEntry entry =
            new RequestTraceLogEntry(
                Instant.now(),
                username,
                method(exchange),
                fullRequestUri(exchange.getRequest()),
                exchange.getRequest()
                    .getURI()
                    .getRawQuery(),
                remoteAddress(exchange),
                routeId(exchange),
                payload,
                status,
                Duration.ofNanos(
                    System.nanoTime() - startedAt
                ).toMillis(),
                error == null
                    ? null
                    : error.getClass().getSimpleName(),
                error == null
                    ? null
                    : truncate(error.getMessage())
            );

        try {
            TRACE_LOG.info(objectMapper.writeValueAsString(entry));
        } catch (JsonProcessingException ex) {
            TRACE_LOG.error("Failed to serialize request trace entry", ex);
        }
    }

    private int determineStatus(ServerWebExchange exchange, Throwable error) {
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        if (statusCode != null) {
            return statusCode.value();
        }

        return error == null ? 200 : 500;
    }

    private String method(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        return method == null ? null : method.name();
    }

    private String fullRequestUri(ServerHttpRequest request) {
        URI uri = request.getURI();
        if (uri.isAbsolute()) {
            return uri.toString();
        }

        String scheme = Optional.ofNullable(request.getHeaders().getFirst("X-Forwarded-Proto")).orElse("http");
        return Optional.ofNullable(request.getHeaders().getHost())
            .map(host -> {
                String hostValue = host.getHostString();
                if (host.getPort() > 0) {
                    hostValue = hostValue + ":" + host.getPort();
                }

                String path = Optional.ofNullable(uri.getRawPath()).orElse("");
                String query = uri.getRawQuery();
                String suffix = query == null ? path : path + "?" + query;
                return scheme + "://" + hostValue + suffix;
            })
            .orElse(uri.toString());
    }
    private String truncate(String message) {

        if (message == null) {
            return null;
        }

        if (message.length() <= 2000) {
            return message;
        }

        return message.substring(0, 2000);
    }
    private Mono<Void> capturePayload(
        ServerWebExchange exchange,
        GatewayFilterChain chain,
        String username,
        long startedAt
    ) {

        return DataBufferUtils.join(
                exchange.getRequest().getBody()
            )
            .map(this::bufferToBytes)
            .defaultIfEmpty(new byte[0])
            .flatMap(bodyBytes -> {

                JsonNode payload =
                    resolvePayload(
                        exchange.getRequest()
                            .getHeaders()
                            .getContentType(),
                        bodyBytes
                    );

                ServerWebExchange decorated =
                    exchange.mutate()
                        .request(
                            decorateRequest(
                                exchange,
                                bodyBytes
                            )
                        )
                        .build();

                return chain.filter(decorated)
                    .doOnSuccess(v ->
                        logTrace(
                            decorated,
                            username,
                            payload,
                            null,
                            startedAt
                        )
                    )
                    .doOnError(ex ->
                        logTrace(
                            decorated,
                            username,
                            payload,
                            ex,
                            startedAt
                        )
                    );
            });
    }

    private String routeId(ServerWebExchange exchange) {

        Route route =
            exchange.getAttribute(
                ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR
            );

        return route == null
            ? null
            : route.getId();
    }
    private String remoteAddress(ServerWebExchange exchange) {

        return Optional.ofNullable(
                exchange.getRequest().getRemoteAddress()
            )
            .map(address -> {
                String ip =
                    address.getAddress().getHostAddress();

                return "0:0:0:0:0:0:0:1".equals(ip)
                    ? "127.0.0.1"
                    : ip;
            })
            .orElse(null);
    }

}






