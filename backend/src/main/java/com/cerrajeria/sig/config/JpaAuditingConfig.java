package com.cerrajeria.sig.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita @CreatedDate / @LastModifiedDate en las entidades que extienden
 * BaseAuditable (ver common.audit). Ver Fase 5, seccion 2.3.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
