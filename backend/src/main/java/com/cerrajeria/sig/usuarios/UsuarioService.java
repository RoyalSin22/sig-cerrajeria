package com.cerrajeria.sig.usuarios;

import com.cerrajeria.sig.usuarios.dto.RestablecerPasswordRequest;
import com.cerrajeria.sig.usuarios.dto.CambiarPasswordRequest;
import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.common.audit.DetalleAuditoria;
import com.cerrajeria.sig.common.audit.AuditAccion;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.usuarios.dto.CambiarEstadoUsuarioRequest;
import com.cerrajeria.sig.usuarios.dto.UsuarioCreateRequest;
import com.cerrajeria.sig.usuarios.dto.UsuarioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Gestion de usuarios (ADMIN/EMPLOYEE). Solo un ADMIN puede crear o desactivar usuarios. */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        usuarioRepository.findByUsername(request.username()).ifPresent(u -> {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre: " + request.username());
        });

        Role role;
        try {
            role = Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Rol inválido: use ROLE_ADMIN o ROLE_EMPLOYEE");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setNombre(request.nombre());
        usuario.setRole(role);
        usuario.setActivo(true);
        usuario = usuarioRepository.save(usuario);

        auditLogService.registrar(usuarioEnSesion(), AuditAccion.CREAR, "Usuario", usuario.getId(),
            DetalleAuditoria.de("Usuario creado: " + usuario.getUsername() + " (" + usuario.getNombre() + ")")
                .cambio("rol", null, role.name()));
        return toResponse(usuario);
    }

    private Usuario usuarioEnSesion() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication().getName();
        return usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + username));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, CambiarEstadoUsuarioRequest request, String usernameActual) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
        // Con un solo administrador, desactivarse a si mismo deja el sistema sin
        // nadie que pueda gestionar usuarios (solo se arreglaria editando la base).
        if (!request.activo() && usuario.getUsername().equals(usernameActual)) {
            throw new IllegalArgumentException("No puedes desactivar tu propio usuario");
        }
        boolean antes = Boolean.TRUE.equals(usuario.getActivo());
        usuario.setActivo(request.activo());
        usuario = usuarioRepository.save(usuario);
        if (antes != request.activo()) {
            auditLogService.registrar(usuarioEnSesion(), AuditAccion.EDITAR, "Usuario", usuario.getId(),
                DetalleAuditoria.de("Usuario " + usuario.getUsername() + (request.activo() ? " reactivado" : " desactivado"))
                    .cambio("activo", antes, request.activo()));
        }
        return toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse vincularWhatsapp(Long id, com.cerrajeria.sig.usuarios.dto.VincularWhatsappRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));

        usuarioRepository.findByTelefonoWhatsappAndActivoTrue(request.telefonoWhatsapp()).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw new IllegalArgumentException("Ese teléfono ya está vinculado a otro usuario");
            }
        });

        usuario.setTelefonoWhatsapp(request.telefonoWhatsapp());
        return toResponse(usuarioRepository.save(usuario));
    }

    /**
     * Cambio de la propia contrasena: exige la actual, para que una sesion
     * abierta y desatendida en el mostrador no baste para quedarse con la cuenta.
     */
    @Transactional
    public void cambiarPasswordPropia(String username, CambiarPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + username));

        // IllegalArgumentException -> 400, no 401: el frontend trata un 401
        // como sesion vencida e intentaria refrescarla.
        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.passwordNueva(), usuario.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta de la actual");
        }

        aplicarPassword(usuario, request.passwordNueva());
        auditLogService.registrar(usuario, AuditAccion.CAMBIO_PASSWORD, "Usuario", usuario.getId());
    }

    /** El administrador restablece la contrasena de otro usuario (ej. el empleado la olvido). */
    @Transactional
    public void restablecerPassword(Long id, RestablecerPasswordRequest request, String usernameAdmin) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
        Usuario admin = usuarioRepository.findByUsername(usernameAdmin)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + usernameAdmin));

        aplicarPassword(usuario, request.passwordNueva());
        auditLogService.registrar(admin, AuditAccion.CAMBIO_PASSWORD, "Usuario", usuario.getId());
    }

    private void aplicarPassword(Usuario usuario, String passwordNueva) {
        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        // Cierra las sesiones abiertas (ver AuthController#refresh). Se trunca a
        // segundos porque el "iat" del JWT tiene precision de segundos: sin esto,
        // un login en el mismo segundo del cambio quedaria rechazado.
        usuario.setPasswordCambiadoEn(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        usuarioRepository.save(usuario);
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getNombre(), u.getRole().name(), u.getActivo(), u.getTelefonoWhatsapp());
    }
}
