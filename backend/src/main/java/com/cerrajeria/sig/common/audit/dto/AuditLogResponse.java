package com.cerrajeria.sig.common.audit.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
    Long id,
    String usuario,
    String accion,
    String entidad,
    Long entidadId,
    LocalDateTime fechaHora
) {
}
