package com.cerrajeria.sig.caja;

import com.cerrajeria.sig.caja.dto.CierreCajaRequest;
import com.cerrajeria.sig.caja.dto.CierreCajaResponse;
import com.cerrajeria.sig.common.MetodoPago;
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

        cajaService.sumarIngresoACajaActiva(caja, new BigDecimal("50.00"), MetodoPago.EFECTIVO);
        cajaService.sumarIngresoACajaActiva(caja, new BigDecimal("30.00"), MetodoPago.EFECTIVO);

        assertThat(caja.getMontoIngresos()).isEqualByComparingTo("80.00");
        assertThat(caja.getMontoEgresos()).isEqualByComparingTo("0.00");
        assertThat(caja.saldoEsperado()).isEqualByComparingTo("180.00"); // apertura + ingresos
        assertThat(caja.efectivoEsperado()).isEqualByComparingTo("180.00");
    }

    @Test
    @DisplayName("Los cobros por Yape y Plin suman al total pero no al efectivo esperado en el cajón")
    void cobrosDigitales_noCuentanComoEfectivo() {
        CajaDiaria caja = cajaAbierta("50.00");

        cajaService.sumarIngresoACajaActiva(caja, new BigDecimal("23.00"), MetodoPago.EFECTIVO);
        cajaService.sumarIngresoACajaActiva(caja, new BigDecimal("85.00"), MetodoPago.YAPE);
        cajaService.sumarIngresoACajaActiva(caja, new BigDecimal("40.00"), MetodoPago.PLIN);

        assertThat(caja.getMontoIngresos()).isEqualByComparingTo("148.00");
        assertThat(caja.getMontoYape()).isEqualByComparingTo("85.00");
        assertThat(caja.getMontoPlin()).isEqualByComparingTo("40.00");
        assertThat(caja.saldoEsperado()).isEqualByComparingTo("198.00");
        assertThat(caja.efectivoEsperado()).isEqualByComparingTo("73.00"); // 50 inicial + 23 en efectivo
    }

    @Test
    @DisplayName("Anular un cobro por Yape lo descuenta de Yape, no del efectivo")
    void anulacionYape_noAfectaEfectivo() {
        CajaDiaria caja = cajaAbierta("50.00");
        cajaService.sumarIngresoACajaActiva(caja, new BigDecimal("85.00"), MetodoPago.YAPE);

        cajaService.sumarEgresoACajaActiva(caja, new BigDecimal("85.00"), MetodoPago.YAPE);

        assertThat(caja.getMontoYape()).isEqualByComparingTo("0.00");
        assertThat(caja.efectivoEsperado()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Un pago MIXTO sin desglosar se rechaza: no se sabría cuánto entró al cajón")
    void pagoMixto_sinDesglose_seRechaza() {
        CajaDiaria caja = cajaAbierta("50.00");

        assertThatThrownBy(() -> cajaService.sumarIngresoACajaActiva(caja, new BigDecimal("10.00"), MetodoPago.MIXTO))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(caja.getMontoIngresos()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("El cierre cuadra contra el efectivo esperado, no contra el total con Yape")
    void cierre_cuadraContraEfectivo() {
        CajaDiaria caja = cajaAbierta("50.00");
        caja.sumarIngreso(new BigDecimal("23.00"), MetodoPago.EFECTIVO);
        caja.sumarIngreso(new BigDecimal("85.00"), MetodoPago.YAPE);
        when(cajaDiariaRepository.findByUsuarioUsernameAndEstado("empleado1", EstadoCaja.ABIERTA))
            .thenReturn(Optional.of(caja));

        CierreCajaResponse cierre = cajaService.cerrar(new CierreCajaRequest(new BigDecimal("73.00")));

        assertThat(cierre.diferencia()).isEqualByComparingTo("0.00");
        assertThat(caja.getEstado()).isEqualTo(EstadoCaja.CERRADA);
    }

    private CajaDiaria cajaAbierta(String montoApertura) {
        CajaDiaria caja = new CajaDiaria();
        caja.setMontoApertura(new BigDecimal(montoApertura));
        caja.setEstado(EstadoCaja.ABIERTA);
        return caja;
    }
}
