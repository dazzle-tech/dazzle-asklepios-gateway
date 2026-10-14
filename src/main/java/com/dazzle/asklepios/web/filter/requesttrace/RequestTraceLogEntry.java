package com.dazzle.asklepios.web.filter.requesttrace;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Structured request trace log entry.
 */

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record RequestTraceLogEntry(
    Instant timestamp,
    String user,
    String method,
    String path,
    String queryString,
    String remoteAddress,
    String routeId,
    JsonNode payload,
    int status,
    long durationMs,
    String exception,
    String exceptionMessage

) {}

