package com.cerrajeria.sig.domicilio;

import com.cerrajeria.sig.caja.CajaDiaria;
import com.cerrajeria.sig.caja.CajaService;
import com.cerrajeria.sig.clientes.ClienteRepository;
import com.cerrajeria.sig.common.MetodoPago;
import com.cerrajeria.sig.common.exception.CajaNoAbiertaException;
import com.cerrajeria.sig.domicilio.dto.CambiarEstadoServicioRequest;
import com.cerrajeria.sig.domicilio.dto.RealizarServicioRequest;
import com.cerrajeria.sig.usuarios.Usuario;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobro de servicios a domicilio: al marcarlo REALIZADO, quien lo hizo cobra
 * en SU caja abierta (regla de negocio 1), con el metodo de pago para el cuadre.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServicioDomicilioService - cobro al realizar (regla de negocio 1)")
class ServicioDomicilioServiceTest {

    @Mock private ServicioDomicilioRepository repository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ServicioDomicilioMapper mapper;
    @Mock private CajaService cajaService;
    @Mock private com.cerrajeria.sig.common.audit.AuditLogService auditLogService;

    @InjectMocks
    private ServicioDomicilioService service;

    private ServicioDomicilio servicio;
    private final Usuario tecnico = new Usuario();

    @BeforeEach
    void preparar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("empleado1", null));
        tecnico.setUsername("empleado1");
        lenient().when(usuarioRepository.findByUsername("empleado1")).thenReturn(Optional.of(tecnico));
        servicio = new ServicioDomicilio();
        servicio.setId(7L);
        servicio.setEstado(EstadoServicio.EN_CAMINO);
        when(repository.findById(7L)).thenReturn(Optional.of(servicio));
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Con monto, cobra en la caja de quien lo realiza con su método de pago")
    void realizar_cobraEnLaCajaDelTecnico() {
        CajaDiaria caja = new CajaDiaria();
        when(cajaService.obtenerCajaAbiertaEntidad()).thenReturn(caja);

        service.realizar(7L, new RealizarServicioRequest(new BigDecimal("80.00"), "yape"));

        verify(cajaService).sumarIngresoACajaActiva(caja, new BigDecimal("80.00"), MetodoPago.YAPE);
        assertThat(servicio.getEstado()).isEqualTo(EstadoServicio.REALIZADO);
        assertThat(servicio.getCajaDiaria()).isSameAs(caja);
        assertThat(servicio.getMetodoPago()).isEqualTo(MetodoPago.YAPE);
        assertThat(servicio.getRealizadoPor()).isSameAs(tecnico);
        assertThat(servicio.getFechaRealizado()).isNotNull();
    }

    @Test
    @DisplayName("Sin caja abierta no se puede cobrar, y el servicio sigue pendiente")
    void realizar_sinCaja_noCobra() {
        when(cajaService.obtenerCajaAbiertaEntidad()).thenThrow(new CajaNoAbiertaException("sin caja"));

        assertThatThrownBy(() -> service.realizar(7L, new RealizarServicioRequest(new BigDecimal("50"), "EFECTIVO")))
            .isInstanceOf(CajaNoAbiertaException.class);
        assertThat(servicio.getEstado()).isEqualTo(EstadoServicio.EN_CAMINO);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Monto 0 (garantía, cortesía): no exige caja ni método de pago")
    void realizar_sinCobro_noPasaPorCaja() {
        service.realizar(7L, new RealizarServicioRequest(BigDecimal.ZERO, null));

        verify(cajaService, never()).obtenerCajaAbiertaEntidad();
        assertThat(servicio.getEstado()).isEqualTo(EstadoServicio.REALIZADO);
        assertThat(servicio.getMontoCobrado()).isEqualByComparingTo("0");
        assertThat(servicio.getMetodoPago()).isNull();
    }

    @Test
    @DisplayName("Con monto exige método de pago, y no admite pago mixto")
    void realizar_exigeMetodoValido() {
        assertThatThrownBy(() -> service.realizar(7L, new RealizarServicioRequest(BigDecimal.TEN, null)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.realizar(7L, new RealizarServicioRequest(BigDecimal.TEN, "MIXTO")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mixto");
        verify(cajaService, never()).sumarIngresoACajaActiva(any(), any(), any());
    }

    @Test
    @DisplayName("Un servicio realizado no se vuelve a cobrar ni cambia de estado")
    void realizado_esFinal() {
        servicio.setEstado(EstadoServicio.REALIZADO);

        assertThatThrownBy(() -> service.realizar(7L, new RealizarServicioRequest(BigDecimal.TEN, "EFECTIVO")))
            .hasMessageContaining("ya fue realizado");
        assertThatThrownBy(() -> service.cambiarEstado(7L, new CambiarEstadoServicioRequest("CANCELADO")))
            .hasMessageContaining("ya fue realizado");
        verify(cajaService, never()).sumarIngresoACajaActiva(any(), any(), any());
    }

    @Test
    @DisplayName("El cambio de estado simple no permite marcar REALIZADO sin cobro")
    void cambiarEstado_noPermiteRealizadoSinCobro() {
        assertThatThrownBy(() -> service.cambiarEstado(7L, new CambiarEstadoServicioRequest("REALIZADO")))
            .hasMessageContaining("cobrado");
        assertThat(servicio.getEstado()).isEqualTo(EstadoServicio.EN_CAMINO);
    }
}
