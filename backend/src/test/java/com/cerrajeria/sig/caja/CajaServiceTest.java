package com.cerrajeria.sig.caja;

import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.common.exception.CajaNoAbiertaException;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Nucleo de la regla de negocio 1 (Fase 1): ninguna operacion monetaria
 * puede procesarse sin una caja diaria ABIERTA para el usuario en sesion.
 * Ventas, Ordenes y Compras dependen todas de este metodo para cumplirla.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CajaService - regla de negocio 1 (caja abierta obligatoria)")
class CajaServiceTest {

    @Mock private CajaDiariaRepository cajaDiariaRepository;
    @Mock private MovimientoCajaRepository movimientoCajaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CajaMapper cajaMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private CajaService cajaService;

    @BeforeEach
    void autenticarUsuario() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("empleado1", null));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("obtenerCajaAbiertaEntidad lanza CajaNoAbiertaException si el usuario no tiene caja abierta")
    void rechaza_cuandoNoHayCajaAbierta() {
        when(cajaDiariaRepository.findByUsuarioUsernameAndEstado("empleado1", EstadoCaja.ABIERTA))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cajaService.obtenerCajaAbiertaEntidad())
            .isInstanceOf(CajaNoAbiertaException.class);
    }

    @Test
    @DisplayName("obtenerCajaAbiertaEntidad devuelve la caja cuando sí existe una abierta para el usuario")
    void permite_cuandoHayCajaAbiertaDelUsuario() {
        CajaDiaria caja = new CajaDiaria();
        caja.setId(7L);
        caja.setMontoApertura(new BigDecimal("100.00"));
        caja.setEstado(EstadoCaja.ABIERTA);

        when(cajaDiariaRepository.findByUsuarioUsernameAndEstado("empleado1", EstadoCaja.ABIERTA))
            .thenReturn(Optional.of(caja));

        CajaDiaria resultado = cajaService.obtenerCajaAbiertaEntidad();

        assertThat(resultado.getId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("sumarIngresoACajaActiva acumula el monto en montoIngresos sin tocar montoEgresos")
    void sumarIngreso_acumulaCorrectamente() {
        CajaDiaria caja = new CajaDiaria();
        caja.setMontoApertura(new BigDecimal("100.00"));
        caja.setEstado(EstadoCaja.ABIERTA);

        cajaService.sumarIngresoACajaActiva(caja, new BigDecimal("50.00"));
        cajaService.sumarIngresoACajaActiva(caja, new BigDecimal("30.00"));

        assertThat(caja.getMontoIngresos()).isEqualByComparingTo("80.00");
        assertThat(caja.getMontoEgresos()).isEqualByComparingTo("0.00");
        assertThat(caja.saldoEsperado()).isEqualByComparingTo("180.00"); // apertura + ingresos
    }
}
