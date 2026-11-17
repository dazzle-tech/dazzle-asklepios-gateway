package com.dazzle.asklepios.repository;

import com.dazzle.asklepios.domain.SystemConfiguration;
import com.dazzle.asklepios.domain.enumeration.SystemConfigKey;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface SystemConfigurationRepository extends R2dbcRepository<SystemConfiguration, Long> {
    Mono<SystemConfiguration> findByConfigKey(SystemConfigKey configKey);
}
