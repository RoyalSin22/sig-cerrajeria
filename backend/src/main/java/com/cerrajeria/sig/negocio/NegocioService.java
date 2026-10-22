package com.cerrajeria.sig.negocio;

import com.cerrajeria.sig.common.audit.AuditAccion;
import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.common.audit.DetalleAuditoria;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.negocio.dto.NegocioDto;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NegocioService {

    private final NegocioRepository negocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public NegocioDto obtener() {
        Negocio n = buscar();
        return new NegocioDto(n.getNombre(), n.getTelefono(), n.getDireccion());
    }

    @Transactional
    public NegocioDto guardar(NegocioDto datos) {
        Negocio n = buscar();
        String telefono = datos.telefono().replaceAll("[^0-9+]", "");
        DetalleAuditoria detalle = DetalleAuditoria.de("Datos del negocio")
            .cambio("nombre", n.getNombre(), datos.nombre().trim())
            .cambio("telefono", n.getTelefono(), telefono)
            .cambio("direccion", n.getDireccion(), datos.direccion().trim());
        n.setNombre(datos.nombre().trim());
        n.setTelefono(telefono);
        n.setDireccion(datos.direccion().trim());
        n.setUpdatedAt(LocalDateTime.now());
        negocioRepository.save(n);
        if (!detalle.sinCambios()) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            usuarioRepository.findByUsername(username).ifPresent(u ->
                auditLogService.registrar(u, AuditAccion.EDITAR, "Negocio", 1L, detalle));
        }
        return obtener();
    }

    private Negocio buscar() {
        return negocioRepository.findById(1)
            .orElseThrow(() -> new RecursoNoEncontradoException("Faltan los datos del negocio (migración V12)"));
    }
}
