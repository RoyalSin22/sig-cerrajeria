package com.cerrajeria.sig.ordenes;

import com.cerrajeria.sig.clientes.Cliente;
import com.cerrajeria.sig.clientes.ClienteRepository;
import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.common.exception.SaldoPendienteException;
import com.cerrajeria.sig.usuarios.Role;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import com.cerrajeria.sig.ordenes.dto.RegistrarPagoOrdenRequest;
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
}
