package com.cerrajeria.sig.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntidadAndEntidadIdOrderByFechaHoraDesc(String entidad, Long entidadId);
    List<AuditLog> findAllByOrderByFechaHoraDesc();
    List<AuditLog> findTop10ByOrderByFechaHoraDesc();

    org.springframework.data.domain.Page<AuditLog> findAllByOrderByFechaHoraDesc(
        org.springframework.data.domain.Pageable pageable);
}
