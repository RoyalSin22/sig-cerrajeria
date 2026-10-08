package com.cerrajeria.sig.usuarios;

import com.cerrajeria.sig.common.audit.AuditAccion;
import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.usuarios.dto.CambiarEstadoUsuarioRequest;
import com.cerrajeria.sig.usuarios.dto.CambiarPasswordRequest;
import com.cerrajeria.sig.usuarios.dto.RestablecerPasswordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cambio de contrasena: es lo que permite reemplazar la contrasena inicial
 * (admin/admin123, publicada en la documentacion) antes de usar el sistema.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - cambio y restablecimiento de contraseña")
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditLogService auditLogService;
    @Spy private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario admin;

    @BeforeEach
    void crearAdmin() {
        admin = new Usuario();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(Role.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Con la contraseña actual correcta, guarda la nueva cifrada y marca el cambio")
    void cambiaPassword_conActualCorrecta() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        Instant antes = Instant.now().minusSeconds(1);

        usuarioService.cambiarPasswordPropia("admin", new CambiarPasswordRequest("admin123", "NuevaClave2026"));

        assertThat(passwordEncoder.matches("NuevaClave2026", admin.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("admin123", admin.getPassword())).isFalse();
        assertThat(admin.getPasswordCambiadoEn()).isAfter(antes); // invalida las sesiones abiertas
        verify(usuarioRepository).save(admin);
        verify(auditLogService).registrar(admin, AuditAccion.CAMBIO_PASSWORD, "Usuario", 1L);
    }

    @Test
    @DisplayName("Con la contraseña actual incorrecta, rechaza y no guarda nada")
    void rechaza_conActualIncorrecta() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        String hashOriginal = admin.getPassword();

        assertThatThrownBy(() -> usuarioService.cambiarPasswordPropia(
                "admin", new CambiarPasswordRequest("adivinada", "NuevaClave2026")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("actual no es correcta");

        assertThat(admin.getPassword()).isEqualTo(hashOriginal);
        assertThat(admin.getPasswordCambiadoEn()).isNull();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza una nueva contraseña igual a la actual")
    void rechaza_nuevaIgualALaActual() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.cambiarPasswordPropia(
                "admin", new CambiarPasswordRequest("admin123", "admin123")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("distinta");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un usuario no puede desactivarse a sí mismo (dejaría el sistema sin administrador)")
    void noPuedeDesactivarseASiMismo() {
        admin.setActivo(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.cambiarEstado(1L, new CambiarEstadoUsuarioRequest(false), "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("propio usuario");
        assertThat(admin.getActivo()).isTrue();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("El administrador restablece la contraseña de otro usuario sin conocer la anterior")
    void adminRestablecePassword() {
        Usuario empleado = new Usuario();
        empleado.setId(2L);
        empleado.setUsername("empleado1");
        empleado.setPassword(passwordEncoder.encode("olvidada"));
        empleado.setRole(Role.ROLE_EMPLOYEE);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(empleado));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        usuarioService.restablecerPassword(2L, new RestablecerPasswordRequest("Temporal123"), "admin");

        assertThat(passwordEncoder.matches("Temporal123", empleado.getPassword())).isTrue();
        assertThat(empleado.getPasswordCambiadoEn()).isNotNull();
        // La auditoria registra QUIEN lo hizo (el admin) sobre que usuario (el empleado)
        verify(auditLogService).registrar(admin, AuditAccion.CAMBIO_PASSWORD, "Usuario", 2L);
    }
}
