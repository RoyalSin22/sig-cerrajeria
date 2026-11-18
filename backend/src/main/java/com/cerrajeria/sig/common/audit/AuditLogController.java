package com.cerrajeria.sig.common.audit;

import com.cerrajeria.sig.common.audit.dto.AuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.PageImpl;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Auditoría")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final tools.jackson.databind.ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "Registro de auditoría paginado, o filtrado por entidad específica (RF-47)")
    public ResponseEntity<Page<AuditLogResponse>> listar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId,
            @PageableDefault(size = 20) Pageable pageable) {

        if (entidad != null && entidadId != null) {
            List<AuditLogResponse> items = auditLogRepository
                .findByEntidadAndEntidadIdOrderByFechaHoraDesc(entidad, entidadId)
                .stream().map(this::toResponse).toList();
            return ResponseEntity.ok(new PageImpl<>(items, pageable, items.size()));
        }

        return ResponseEntity.ok(auditLogRepository.findAllByOrderByFechaHoraDesc(pageable).map(this::toResponse));
    }

    private AuditLogResponse toResponse(AuditLog l) {
        return new AuditLogResponse(
            l.getId(), l.getUsuario().getNombre(), l.getAccion().name(),
            l.getEntidad(), l.getEntidadId(),
            l.getDetalle() == null ? null : objectMapper.readTree(l.getDetalle()), l.getFechaHora());
    }
}
