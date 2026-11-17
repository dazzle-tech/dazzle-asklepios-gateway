-- Tables owned by other services (general-service) that the gateway's Liquibase
-- report views read from. They are not created by this service's changelog, so the
-- empty integration-test database needs them before Liquibase runs.
-- Columns mirror what reports.vwaplov and reports.vwaplovvalues select.

CREATE TABLE IF NOT EXISTS public.ap_lov (
    key                 varchar(255) PRIMARY KEY,
    lov_code            varchar(255),
    lov_name            varchar(255),
    lov_description     varchar(255),
    love_custom_code    varchar(255),
    parent_lov          varchar(255),
    auto_select_default boolean,
    default_value_id    varchar(255),
    created_by          varchar(255),
    updated_by          varchar(255),
    deleted_by          varchar(255),
    created_at          numeric(16),
    updated_at          numeric(16),
    deleted_at          numeric(16),
    is_valid            boolean
);

CREATE TABLE IF NOT EXISTS public.ap_lov_values (
    key                    varchar(255) PRIMARY KEY,
    lov_key                varchar(255),
    lov_code               varchar(255),
    value_code             varchar(255),
    lov_display_vale       varchar(255),
    love_custom_code       varchar(255),
    value_description      varchar(255),
    value_color            varchar(255),
    value_icon             varchar(255),
    value_order            numeric,
    isdefault              boolean,
    seeded_data            boolean,
    for_internal_user      boolean,
    specific_for_screen_id varchar(255),
    parent_value_id        varchar(255),
    created_by             varchar(255),
    updated_by             varchar(255),
    deleted_by             varchar(255),
    created_at             numeric(16),
    updated_at             numeric(16),
    deleted_at             numeric(16),
    is_valid               boolean,
    score                  numeric
);
