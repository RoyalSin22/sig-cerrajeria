package com.cerrajeria.sig.common.audit.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
    Long id,
    String usuario,
    String accion,
    String entidad,
    Long entidadId,
    /** JSON tal cual se guardo: {descripcion, motivo?, cambios?: {campo: {antes, despues}}} */
    tools.jackson.databind.JsonNode detalle,
    LocalDateTime fechaHora
) {
}
