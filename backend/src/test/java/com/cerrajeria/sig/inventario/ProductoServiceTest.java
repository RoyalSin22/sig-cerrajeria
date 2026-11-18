package com.cerrajeria.sig.inventario;

import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.common.exception.StockInsuficienteException;
import com.cerrajeria.sig.inventario.dto.AjusteStockRequest;
import com.cerrajeria.sig.inventario.dto.ProductoResponse;
import com.cerrajeria.sig.usuarios.Role;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica la regla de negocio 2 (Fase 1): todo cambio de stock genera
 * Kardex, y nunca se permite que el stock quede negativo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService - stock y Kardex (regla de negocio 2)")
class ProductoServiceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProductoMapper productoMapper;
    @Mock private MovimientoInventarioMapper movimientoInventarioMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private ProductoService productoService;

    @BeforeEach
    void autenticarUsuario() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("admin", null));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Producto productoConStock(int cantidadStock) {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setCodigo("KW1-001");
        producto.setNombre("Llave virgen KW1");
        producto.setTipoProducto(TipoProducto.LLAVE_VIRGEN);
        producto.setCantidadStock(cantidadStock);
        producto.setStockMinimo(5);
        producto.setPrecioCompra(new BigDecimal("1.50"));
        producto.setPrecioVenta(new BigDecimal("3.00"));
        producto.setActivo(true);
        return producto;
    }

    @Test
    @DisplayName("descontarStock lanza StockInsuficienteException si la venta pide más de lo disponible")
    void descontarStock_rechaza_cuandoNoAlcanzaElStock() {
        Producto producto = productoConStock(3);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> productoService.descontarStock(1L, 5, ReferenciaTipo.VENTA, 100L))
            .isInstanceOf(StockInsuficienteException.class);

        assertThat(producto.getCantidadStock()).isEqualTo(3); // el stock no debe alterarse
        verify(movimientoInventarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("descontarStock permite exactamente hasta agotar el stock (límite en cero)")
    void descontarStock_permite_dejarStockEnCero() {
        Producto producto = productoConStock(5);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioDePrueba()));

        Producto resultado = productoService.descontarStock(1L, 5, ReferenciaTipo.VENTA, 100L);

        assertThat(resultado.getCantidadStock()).isZero();
        verify(movimientoInventarioRepository).save(any(MovimientoInventario.class));
    }

    @Test
    @DisplayName("ajustarStock rechaza una cantidad de ajuste igual a cero")
    void ajustarStock_rechaza_cantidadCero() {
        var request = new AjusteStockRequest(0, "Conteo físico");

        assertThatThrownBy(() -> productoService.ajustarStock(1L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cero");

        verify(productoRepository, never()).findById(any());
    }

    @Test
    @DisplayName("ajustarStock rechaza un ajuste que dejaría el stock en negativo")
    void ajustarStock_rechaza_siQuedaNegativo() {
        Producto producto = productoConStock(3);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        var request = new AjusteStockRequest(-10, "Merma detectada");

        assertThatThrownBy(() -> productoService.ajustarStock(1L, request))
            .isInstanceOf(StockInsuficienteException.class);

        assertThat(producto.getCantidadStock()).isEqualTo(3);
    }

    @Test
    @DisplayName("ajustarStock exitoso actualiza el stock y registra el Kardex con el motivo dado")
    void ajustarStock_exitoso_generaMovimientoDeKardex() {
        Producto producto = productoConStock(10);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioDePrueba()));
        when(productoMapper.toResponse(producto)).thenReturn(
            new ProductoResponse(1L, "KW1-001", "Llave virgen KW1", "LLAVE_VIRGEN", null,
                15, 5, new BigDecimal("1.50"), new BigDecimal("3.00"), null, null, null, null, null, null, null, null,
                true, false, false));

        var request = new AjusteStockRequest(5, "Ingreso no facturado");
        ProductoResponse respuesta = productoService.ajustarStock(1L, request);

        assertThat(producto.getCantidadStock()).isEqualTo(15);
        assertThat(respuesta.cantidadStock()).isEqualTo(15);
        verify(movimientoInventarioRepository).save(any(MovimientoInventario.class));
        var detalle = org.mockito.ArgumentCaptor.forClass(com.cerrajeria.sig.common.audit.DetalleAuditoria.class);
        verify(auditLogService).registrar(any(Usuario.class), any(), eq("Producto"), eq(1L), detalle.capture());
        assertThat(detalle.getValue().getMotivo()).isEqualTo("Ingreso no facturado");
        assertThat(detalle.getValue().getCambios().get("stock"))
            .isEqualTo(new com.cerrajeria.sig.common.audit.DetalleAuditoria.Cambio("10", "15"));
        // El stock se lee con la fila bloqueada (dos ventas simultaneas no pisan sus restas)
        verify(entityManager).refresh(producto, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    private Usuario usuarioDePrueba() {
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setNombre("Administrador");
        usuario.setRole(Role.ROLE_ADMIN);
        return usuario;
    }
}
