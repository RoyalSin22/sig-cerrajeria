package com.cerrajeria.sig.common.audit;

import com.cerrajeria.sig.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Punto unico de registro de auditoria (RF-47), invocado desde los servicios
 * que ejecutan acciones sensibles: caja, inventario, precios, compras,
 * ordenes, anulaciones y usuarios.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void registrar(Usuario usuario, AuditAccion accion, String entidad, Long entidadId) {
        registrar(usuario, accion, entidad, entidadId, null);
    }

    @Transactional
    public void registrar(Usuario usuario, AuditAccion accion, String entidad, Long entidadId, DetalleAuditoria detalle) {
        AuditLog log = new AuditLog();
        log.setUsuario(usuario);
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setEntidadId(entidadId);
        log.setDetalle(detalle == null ? null : json(detalle));
        auditLogRepository.save(log);
    }

    private String json(DetalleAuditoria detalle) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("descripcion", detalle.getDescripcion());
        if (detalle.getMotivo() != null) mapa.put("motivo", detalle.getMotivo());
        if (!detalle.sinCambios()) mapa.put("cambios", detalle.getCambios());
        return objectMapper.writeValueAsString(mapa);
    }
}
