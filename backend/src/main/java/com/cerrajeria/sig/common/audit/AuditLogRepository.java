package com.cerrajeria.sig.common.audit;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// AuditLogController mapea usuario.nombre fuera de transaccion (open-in-view: false):
// el usuario se carga en la misma consulta para evitar LazyInitializationException.
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @EntityGraph(attributePaths = "usuario")
    List<AuditLog> findByEntidadAndEntidadIdOrderByFechaHoraDesc(String entidad, Long entidadId);

    @EntityGraph(attributePaths = "usuario")
    List<AuditLog> findAllByOrderByFechaHoraDesc();

    @EntityGraph(attributePaths = "usuario")
    List<AuditLog> findTop10ByOrderByFechaHoraDesc();

    @EntityGraph(attributePaths = "usuario")
    org.springframework.data.domain.Page<AuditLog> findAllByOrderByFechaHoraDesc(
        org.springframework.data.domain.Pageable pageable);
}
