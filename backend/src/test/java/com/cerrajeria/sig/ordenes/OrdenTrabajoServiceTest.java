package com.cerrajeria.sig.ordenes;

import com.cerrajeria.sig.clientes.Cliente;
import com.cerrajeria.sig.clientes.ClienteRepository;
import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.common.exception.SaldoPendienteException;
import com.cerrajeria.sig.usuarios.Role;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import com.cerrajeria.sig.ordenes.dto.CancelarOrdenRequest;
import com.cerrajeria.sig.ordenes.dto.CambiarEstadoOrdenRequest;
import com.cerrajeria.sig.ordenes.dto.RegistrarPagoOrdenRequest;
import com.cerrajeria.sig.caja.CajaDiaria;
import com.cerrajeria.sig.common.MetodoPago;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica la regla de negocio 3 (Fase 1): no se puede marcar una orden
 * ENTREGADA con saldo pendiente sin liquidarlo en la misma operacion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrdenTrabajoService - regla de negocio 3 (bloqueo de entrega con saldo pendiente)")
class OrdenTrabajoServiceTest {

    @Mock private OrdenTrabajoRepository ordenTrabajoRepository;
    @Mock private PagoOrdenRepository pagoOrdenRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private com.cerrajeria.sig.caja.CajaService cajaService;
    @Mock private AuditLogService auditLogService;
    @Mock private com.cerrajeria.sig.inventario.ProductoService productoService;

    @InjectMocks
    private OrdenTrabajoService ordenTrabajoService;

    @BeforeEach
    void autenticarUsuario() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private OrdenTrabajo ordenConSaldo(BigDecimal precioTotal, BigDecimal montoPagado) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Juan Pérez");

        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setId(1L);
        orden.setNumeroOrden("OT-000001");
        orden.setCliente(cliente);
        orden.setProductoRecibido("Chapa Yale");
        orden.setProblema("No abre");
        orden.setPrecioTotal(precioTotal);
        orden.setMontoPagado(montoPagado);
        orden.setEstado(EstadoOrden.LISTO);
        return orden;
    }

    @Test
    @DisplayName("Rechaza la entrega si hay saldo pendiente y no se envía ningún pago")
    void entregar_rechaza_sinPagoYConSaldo() {
        OrdenTrabajo orden = ordenConSaldo(new BigDecimal("150.00"), BigDecimal.ZERO);
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));

        assertThatThrownBy(() -> ordenTrabajoService.entregarConLiquidacion(1L, null))
            .isInstanceOf(SaldoPendienteException.class)
            .hasMessageContaining("150.00");

        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.LISTO); // nunca cambia de estado
    }

    @Test
    @DisplayName("Rechaza la entrega si el pago enviado es menor al saldo pendiente")
    void entregar_rechaza_pagoParcialMenorAlSaldo() {
        OrdenTrabajo orden = ordenConSaldo(new BigDecimal("150.00"), new BigDecimal("50.00")); // saldo = 100
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));

        var pagoInsuficiente = new RegistrarPagoOrdenRequest(new BigDecimal("99.99"), "EFECTIVO");

        assertThatThrownBy(() -> ordenTrabajoService.entregarConLiquidacion(1L, pagoInsuficiente))
            .isInstanceOf(SaldoPendienteException.class);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.LISTO);
    }

    @Test
    @DisplayName("Permite la entrega directa cuando no hay saldo pendiente (sin exigir pago)")
    void entregar_permite_sinSaldoPendiente() {
        OrdenTrabajo orden = ordenConSaldo(new BigDecimal("150.00"), new BigDecimal("150.00")); // saldo = 0
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioDePrueba()));

        var respuesta = ordenTrabajoService.entregarConLiquidacion(1L, null);

        assertThat(respuesta.estado()).isEqualTo("ENTREGADO");
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.ENTREGADO);
    }

    @Test
    @DisplayName("Permite la entrega cuando el pago enviado liquida exactamente el saldo pendiente")
    void entregar_permite_pagoExactoQueLiquidaElSaldo() {
        OrdenTrabajo orden = ordenConSaldo(new BigDecimal("150.00"), new BigDecimal("50.00")); // saldo = 100
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenTrabajoRepository.save(any(OrdenTrabajo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioDePrueba()));
        when(cajaService.obtenerCajaAbiertaEntidad()).thenReturn(cajaDePrueba());

        var pagoExacto = new RegistrarPagoOrdenRequest(new BigDecimal("100.00"), "EFECTIVO");
        var respuesta = ordenTrabajoService.entregarConLiquidacion(1L, pagoExacto);

        assertThat(respuesta.estado()).isEqualTo("ENTREGADO");
        assertThat(orden.getMontoPagado()).isEqualByComparingTo("150.00");
    }

    private Usuario usuarioDePrueba() {
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setNombre("Administrador");
        usuario.setRole(Role.ROLE_ADMIN);
        return usuario;
    }

    private com.cerrajeria.sig.caja.CajaDiaria cajaDePrueba() {
        com.cerrajeria.sig.caja.CajaDiaria caja = new com.cerrajeria.sig.caja.CajaDiaria();
        caja.setId(1L);
        caja.setMontoApertura(BigDecimal.ZERO);
        caja.setEstado(com.cerrajeria.sig.caja.EstadoCaja.ABIERTA);
        return caja;
    }

    // --- Cancelacion (V9) ---

    @Test
    @DisplayName("Cancelar devolviendo el adelanto: egreso en la caja de quien devuelve y pago DEVOLUCION")
    void cancelar_devolviendoAdelanto() {
        OrdenTrabajo orden = ordenConSaldo(new BigDecimal("150.00"), new BigDecimal("50.00"));
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenTrabajoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(new Usuario()));
        CajaDiaria caja = new CajaDiaria();
        when(cajaService.obtenerCajaAbiertaEntidad()).thenReturn(caja);

        ordenTrabajoService.cancelar(1L, new CancelarOrdenRequest("El cliente se arrepintió", true, "EFECTIVO"));

        verify(cajaService).sumarEgresoACajaActiva(caja, new BigDecimal("50.00"), MetodoPago.EFECTIVO);
        ArgumentCaptor<PagoOrden> pago = ArgumentCaptor.forClass(PagoOrden.class);
        verify(pagoOrdenRepository).save(pago.capture());
        assertThat(pago.getValue().getTipoPago()).isEqualTo(TipoPagoOrden.DEVOLUCION);
        assertThat(pago.getValue().getMonto()).isEqualByComparingTo("50.00");
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.CANCELADO);
        assertThat(orden.getMontoPagado()).isEqualByComparingTo("0");
        assertThat(orden.getMotivoCancelacion()).isEqualTo("El cliente se arrepintió");
    }

    @Test
    @DisplayName("Cancelar sin devolver (el cliente nunca la recogió): no toca la caja")
    void cancelar_sinDevolver_noTocaCaja() {
        OrdenTrabajo orden = ordenConSaldo(new BigDecimal("150.00"), new BigDecimal("50.00"));
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenTrabajoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(new Usuario()));

        ordenTrabajoService.cancelar(1L, new CancelarOrdenRequest("Abandonada", false, null));

        verify(cajaService, never()).obtenerCajaAbiertaEntidad();
        verify(pagoOrdenRepository, never()).save(any());
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.CANCELADO);
        assertThat(orden.getMontoPagado()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("No se cancela una orden entregada, ni se cobra o cambia de estado una cancelada")
    void estadosFinales_bloqueados() {
        OrdenTrabajo entregada = ordenConSaldo(new BigDecimal("100"), new BigDecimal("100"));
        entregada.setEstado(EstadoOrden.ENTREGADO);
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(entregada));
        assertThatThrownBy(() -> ordenTrabajoService.cancelar(1L, new CancelarOrdenRequest("x", true, "EFECTIVO")))
            .hasMessageContaining("ya fue entregada");

        entregada.setEstado(EstadoOrden.CANCELADO);
        assertThatThrownBy(() -> ordenTrabajoService.registrarAdelanto(1L,
                new RegistrarPagoOrdenRequest(BigDecimal.ONE, "EFECTIVO")))
            .hasMessageContaining("cancelada");
        assertThatThrownBy(() -> ordenTrabajoService.cambiarEstado(1L, new CambiarEstadoOrdenRequest("LISTO")))
            .hasMessageContaining("cancelada");
        verify(cajaService, never()).obtenerCajaAbiertaEntidad();
    }

    @Test
    @DisplayName("El cambio de estado simple no permite cancelar sin motivo")
    void cambiarEstado_noCancela() {
        OrdenTrabajo orden = ordenConSaldo(new BigDecimal("100"), BigDecimal.ZERO);
        when(ordenTrabajoRepository.findById(1L)).thenReturn(Optional.of(orden));
        assertThatThrownBy(() -> ordenTrabajoService.cambiarEstado(1L, new CambiarEstadoOrdenRequest("CANCELADO")))
            .hasMessageContaining("motivo");
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.LISTO);
    }
}
