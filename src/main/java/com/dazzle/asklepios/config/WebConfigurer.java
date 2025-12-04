package com.dazzle.asklepios.config;

import com.dazzle.asklepios.web.rest.errors.ExceptionTranslator;
import com.dazzle.asklepios.web.rest.errors.ReactiveWebExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver;
import org.springframework.data.web.ReactiveSortHandlerMethodArgumentResolver;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.WebExceptionHandler;

import java.util.Arrays;

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
@Configuration
public class WebConfigurer implements WebFluxConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(WebConfigurer.class);

    private final CorsConfigProperties corsConfigProperties;

    public WebConfigurer(CorsConfigProperties corsConfigProperties) {
        this.corsConfigProperties = corsConfigProperties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        if (StringUtils.hasText(corsConfigProperties.getAllowedOrigins()) ||
            StringUtils.hasText(corsConfigProperties.getAllowedOriginPatterns())) {

            if (StringUtils.hasText(corsConfigProperties.getAllowedOrigins())) {
                config.setAllowedOrigins(Arrays.asList(corsConfigProperties.getAllowedOrigins().split(",")));
            }

            if (StringUtils.hasText(corsConfigProperties.getAllowedOriginPatterns())) {
                config.setAllowedOriginPatterns(Arrays.asList(corsConfigProperties.getAllowedOriginPatterns().split(",")));
            }

            config.setAllowedMethods(Arrays.asList(corsConfigProperties.getAllowedMethods().split(",")));
            config.setAllowedHeaders(Arrays.asList(corsConfigProperties.getAllowedHeaders().split(",")));
            config.setExposedHeaders(Arrays.asList(corsConfigProperties.getExposedHeaders().split(",")));
            config.setAllowCredentials(corsConfigProperties.isAllowCredentials());
            config.setMaxAge((long) corsConfigProperties.getMaxAge());

            LOG.debug("Registering CORS filter");
            source.registerCorsConfiguration("/api/**", config);
            source.registerCorsConfiguration("/management/**", config);
            source.registerCorsConfiguration("/setup-service/v3/api-docs", config);
            source.registerCorsConfiguration("/v3/api-docs", config);
            source.registerCorsConfiguration("/swagger-ui/**", config);
            source.registerCorsConfiguration("/*/api/**", config);
            source.registerCorsConfiguration("/services/*/api/**", config);
            source.registerCorsConfiguration("/*/management/**", config);
            source.registerCorsConfiguration("/reference-data/**", config);
            source.registerCorsConfiguration("/appointment/**", config);
            source.registerCorsConfiguration("/attachment/**", config);
            source.registerCorsConfiguration("/dental/**", config);
            source.registerCorsConfiguration("/dvm/**", config);
            source.registerCorsConfiguration("/encounter/**", config);
            source.registerCorsConfiguration("/general/**", config);
            source.registerCorsConfiguration("/lab/**", config);
            source.registerCorsConfiguration("/medications/**", config);
            source.registerCorsConfiguration("/observation/**", config);
            source.registerCorsConfiguration("/pas/**", config);
            source.registerCorsConfiguration("/rad/**", config);
            source.registerCorsConfiguration("/setup/**", config);
            source.registerCorsConfiguration("/auth/**", config);
            source.registerCorsConfiguration("/transaction/**", config);
            source.registerCorsConfiguration("/operation/**", config);
            source.registerCorsConfiguration("/procedures/**", config);
            source.registerCorsConfiguration("/api/patient/**",config);
            source.registerCorsConfiguration("/api/inventory/**" ,config);
            source.registerCorsConfiguration("/api/billing/**" ,config);

        }
        return source;
    }

    // TODO: remove when this is supported in spring-boot
    @Bean
    HandlerMethodArgumentResolver reactivePageableHandlerMethodArgumentResolver() {
        return new ReactivePageableHandlerMethodArgumentResolver();
    }

    // TODO: remove when this is supported in spring-boot
    @Bean
    HandlerMethodArgumentResolver reactiveSortHandlerMethodArgumentResolver() {
        return new ReactiveSortHandlerMethodArgumentResolver();
    }

    @Bean
    @Order(-2)
    public WebExceptionHandler problemExceptionHandler(ObjectMapper mapper, ExceptionTranslator problemHandling) {
        return new ReactiveWebExceptionHandler(problemHandling, mapper);
    }
}
