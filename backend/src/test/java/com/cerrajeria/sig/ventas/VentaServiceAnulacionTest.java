package com.cerrajeria.sig.ventas;

import com.cerrajeria.sig.caja.CajaDiaria;
import com.cerrajeria.sig.caja.CajaService;
import com.cerrajeria.sig.caja.EstadoCaja;
import com.cerrajeria.sig.clientes.ClienteRepository;
import com.cerrajeria.sig.common.MetodoPago;
import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.inventario.Producto;
import com.cerrajeria.sig.inventario.ProductoService;
import com.cerrajeria.sig.inventario.ReferenciaTipo;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Anulacion de ventas: devuelve el stock por el Kardex (regla 2) y el dinero
 * por el mismo metodo con que se cobro (regla 1), solo con la caja abierta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VentaService - anulación (reglas de negocio 1 y 2)")
class VentaServiceAnulacionTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private PagoVentaRepository pagoVentaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CajaService cajaService;
    @Mock private ProductoService productoService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private VentaService ventaService;

    private Venta venta;
    private CajaDiaria caja;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));
        Usuario admin = new Usuario();
        admin.setNombre("Admin");
        lenient().when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        caja = new CajaDiaria();
        caja.setEstado(EstadoCaja.ABIERTA);

        Producto llave = new Producto();
        llave.setId(5L);
        llave.setNombre("Llave KW1");
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(llave);
        detalle.setCantidad(3);
        detalle.setPrecioUnitario(new BigDecimal("4.00"));

        venta = new Venta();
        venta.setId(10L);
        venta.setCajaDiaria(caja);
        venta.setUsuario(admin);
        venta.setMontoTotal(new BigDecimal("12.00"));
        venta.setMetodoPago(MetodoPago.YAPE);
        venta.setAnulada(false);
        venta.getDetalles().add(detalle);
        detalle.setVenta(venta);

        when(ventaRepository.findById(10L)).thenReturn(Optional.of(venta));
        lenient().when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Devuelve el stock por el Kárdex y el dinero por el mismo método")
    void anular_reviertStockYCaja() {
        ventaService.anular(10L);

        verify(productoService).incrementarStock(5L, 3, ReferenciaTipo.VENTA, 10L);
        verify(cajaService).sumarEgresoACajaActiva(caja, new BigDecimal("12.00"), MetodoPago.YAPE);
        assertThat(venta.getAnulada()).isTrue();
        assertThat(venta.getFechaAnulacion()).isNotNull();
    }

    @Test
    @DisplayName("Una venta mixta se devuelve línea por línea según su desglose")
    void anular_mixtoPorLinea() {
        venta.setMetodoPago(MetodoPago.MIXTO);
        PagoVenta efectivo = new PagoVenta();
        efectivo.setMetodoPago(MetodoPago.EFECTIVO);
        efectivo.setMonto(new BigDecimal("5.00"));
        PagoVenta yape = new PagoVenta();
        yape.setMetodoPago(MetodoPago.YAPE);
        yape.setMonto(new BigDecimal("7.00"));
        when(pagoVentaRepository.findByVentaId(10L)).thenReturn(List.of(efectivo, yape));

        ventaService.anular(10L);

        verify(cajaService).sumarEgresoACajaActiva(caja, new BigDecimal("5.00"), MetodoPago.EFECTIVO);
        verify(cajaService).sumarEgresoACajaActiva(caja, new BigDecimal("7.00"), MetodoPago.YAPE);
    }

    @Test
    @DisplayName("Con la caja de la venta ya cerrada no se anula, ni se toca stock")
    void anular_cajaCerrada_rechaza() {
        caja.setEstado(EstadoCaja.CERRADA);

        assertThatThrownBy(() -> ventaService.anular(10L)).hasMessageContaining("cerrada");
        verify(productoService, never()).incrementarStock(anyLong(), anyInt(), any(), any());
        verify(cajaService, never()).sumarEgresoACajaActiva(any(), any(), any());
    }

    @Test
    @DisplayName("Una venta ya anulada no se vuelve a anular")
    void anular_dosVeces_rechaza() {
        venta.setAnulada(true);

        assertThatThrownBy(() -> ventaService.anular(10L)).hasMessageContaining("ya se encuentra anulada");
        verify(cajaService, never()).sumarEgresoACajaActiva(any(), any(), any());
    }
}
