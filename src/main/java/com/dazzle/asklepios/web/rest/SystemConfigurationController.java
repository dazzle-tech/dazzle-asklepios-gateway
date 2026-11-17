package com.dazzle.asklepios.web.rest;

import com.dazzle.asklepios.service.SystemConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Reads the system logo configured in setup (theme SYSTEM_LOGO).
 */
@RestController
@RequestMapping("/api/system-config")
public class SystemConfigurationController {

    private static final Logger LOG = LoggerFactory.getLogger(SystemConfigurationController.class);

    private final SystemConfigurationService systemConfigurationService;

    public SystemConfigurationController(SystemConfigurationService systemConfigurationService) {
        this.systemConfigurationService = systemConfigurationService;
    }

    /**
     * {@code GET /api/system-config/logo} : public URL of the configured system logo.
     * Empty when no logo has been uploaded.
     */
    @GetMapping(value = "/logo", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity<String>> getSystemLogo() {
        LOG.info("REST request to get the configured system logo");
        return systemConfigurationService
            .getResolvedSystemLogo()
            .map(ResponseEntity::ok);
    }
}
