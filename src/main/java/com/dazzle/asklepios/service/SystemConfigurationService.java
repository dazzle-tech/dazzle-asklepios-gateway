package com.dazzle.asklepios.service;

import com.dazzle.asklepios.domain.SystemConfiguration;
import com.dazzle.asklepios.domain.enumeration.SystemConfigKey;
import com.dazzle.asklepios.repository.SystemConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Reads system theme configuration stored by the setup service.
 * Image values are storage keys resolved to the public CDN, matching setup's resolved value.
 */
@Service
public class SystemConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(SystemConfigurationService.class);

    private final SystemConfigurationRepository repository;

    @Value("${asklepios.attachments.cdn-endpoint:}")
    private String cdnEndpoint;

    @Value("${asklepios.attachments.endpoint:}")
    private String endpoint;

    public SystemConfigurationService(SystemConfigurationRepository repository) {
        this.repository = repository;
    }

    public Mono<String> getResolvedSystemLogo() {
        LOG.debug("Loading resolved system logo");
        return repository
            .findByConfigKey(SystemConfigKey.SYSTEM_LOGO)
            .map(this::resolveImageValue)
            .defaultIfEmpty("");
    }

    private String resolveImageValue(SystemConfiguration config) {
        String value = config.getConfigValue();
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }

        String baseUrl = firstNonBlank(cdnEndpoint, endpoint);
        if (baseUrl == null) {
            LOG.warn("CDN/endpoint is not configured. Returning raw system logo key");
            return value;
        }

        String resolvedUrl = baseUrl.replaceAll("/$", "") + "/" + value.replaceFirst("^/", "");
        LOG.debug("Resolved system logo url: {}", resolvedUrl);
        return resolvedUrl;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
