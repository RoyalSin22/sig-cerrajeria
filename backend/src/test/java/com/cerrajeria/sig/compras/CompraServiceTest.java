package com.cerrajeria.sig.compras;

import com.cerrajeria.sig.caja.CajaDiaria;
import com.cerrajeria.sig.caja.CajaService;
import com.cerrajeria.sig.common.MetodoPago;
import com.cerrajeria.sig.common.exception.CajaNoAbiertaException;
import com.cerrajeria.sig.compras.dto.CompraCreateRequest;
import com.cerrajeria.sig.compras.dto.CompraLoteRequest;
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
 * Compras: el stock entra por el Kardex (regla 2) y, si se paga de caja, el
 * egreso exige caja abierta (regla 1).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompraService - Kárdex y caja (reglas de negocio 1 y 2)")
class CompraServiceTest {

    @Mock private CompraRepository compraRepository;
    @Mock private ProveedorService proveedorService;
    @Mock private ProductoService productoService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CajaService cajaService;

    @InjectMocks
    private CompraService compraService;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(new Usuario()));
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("Distribuidora Lima");
        when(proveedorService.buscarPorId(1L)).thenReturn(proveedor);
        lenient().when(compraRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Producto producto = new Producto();
        producto.setNombre("Candado 40 mm");
        lenient().when(productoService.incrementarStock(anyLong(), anyInt(), any(), any())).thenReturn(producto);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Pagada de caja: egreso en efectivo por el total y entrada de stock por el Kárdex")
    void compraPagadaDeCaja() {
        CajaDiaria caja = new CajaDiaria();
        when(cajaService.obtenerCajaAbiertaEntidad()).thenReturn(caja);

        compraService.crear(new CompraCreateRequest(1L, 9L, 4, new BigDecimal("15.50"), true));

        verify(cajaService).sumarEgresoACajaActiva(caja, new BigDecimal("62.00"), MetodoPago.EFECTIVO);
        verify(productoService).incrementarStock(9L, 4, ReferenciaTipo.COMPRA, null);
    }

    @Test
    @DisplayName("No pagada de caja: no exige caja, pero el stock igual pasa por el Kárdex")
    void compraSinCaja() {
        compraService.crear(new CompraCreateRequest(1L, 9L, 4, new BigDecimal("15.50"), false));

        verify(cajaService, never()).obtenerCajaAbiertaEntidad();
        verify(productoService).incrementarStock(9L, 4, ReferenciaTipo.COMPRA, null);
    }

    @Test
    @DisplayName("Pagada de caja sin caja abierta: se rechaza y no entra stock")
    void compraPagadaSinCajaAbierta() {
        when(cajaService.obtenerCajaAbiertaEntidad()).thenThrow(new CajaNoAbiertaException("sin caja"));

        assertThatThrownBy(() -> compraService.crear(new CompraCreateRequest(1L, 9L, 4, BigDecimal.TEN, true)))
            .isInstanceOf(CajaNoAbiertaException.class);
        verify(productoService, never()).incrementarStock(anyLong(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Compra de varios productos: cada uno entra por el Kárdex y la caja paga cada línea")
    void compraDeVarios() {
        CajaDiaria caja = new CajaDiaria();
        when(cajaService.obtenerCajaAbiertaEntidad()).thenReturn(caja);

        var respuestas = compraService.crearLote(new CompraLoteRequest(1L, List.of(
            new CompraLoteRequest.Item(9L, 100, new BigDecimal("0.80")),
            new CompraLoteRequest.Item(12L, 3, new BigDecimal("18.00"))), true));

        assertThat(respuestas).hasSize(2);
        verify(productoService).incrementarStock(9L, 100, ReferenciaTipo.COMPRA, null);
        verify(productoService).incrementarStock(12L, 3, ReferenciaTipo.COMPRA, null);
        verify(cajaService).sumarEgresoACajaActiva(caja, new BigDecimal("80.00"), MetodoPago.EFECTIVO);
        verify(cajaService).sumarEgresoACajaActiva(caja, new BigDecimal("54.00"), MetodoPago.EFECTIVO);
    }
}
