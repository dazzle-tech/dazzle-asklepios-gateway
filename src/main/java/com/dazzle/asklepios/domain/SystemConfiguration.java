package com.dazzle.asklepios.domain;

import com.dazzle.asklepios.domain.enumeration.SystemConfigKey;
import com.dazzle.asklepios.domain.enumeration.SystemConfigType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;

/**
 * Theme and branding values maintained by the setup service.
 */
@EqualsAndHashCode(callSuper = true)
@Table("system_configuration")
@Data
public class SystemConfiguration extends AbstractAuditingEntity<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @NotNull
    @Column("config_key")
    private SystemConfigKey configKey;

    @NotNull
    @Column("config_value")
    private String configValue;

    @NotNull
    @Column("config_type")
    private SystemConfigType configType;

    @Column("description")
    private String description;
}
