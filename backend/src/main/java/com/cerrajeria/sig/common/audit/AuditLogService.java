package com.cerrajeria.sig.common.audit;

import com.cerrajeria.sig.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Punto unico de registro de auditoria (RF-47), invocado desde los servicios
 * que ejecutan acciones sensibles: apertura/cierre de caja, ajustes de
 * inventario, cambios de estado de ordenes.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void registrar(Usuario usuario, AuditAccion accion, String entidad, Long entidadId) {
        AuditLog log = new AuditLog();
        log.setUsuario(usuario);
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setEntidadId(entidadId);
        auditLogRepository.save(log);
    }
}
