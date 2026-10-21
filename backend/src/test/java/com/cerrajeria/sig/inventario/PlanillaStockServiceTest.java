package com.cerrajeria.sig.inventario;

import com.cerrajeria.sig.inventario.dto.ResultadoPlanilla;
import com.cerrajeria.sig.inventario.dto.AjusteStockRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Planilla de cantidades: se descarga, se llena en Excel y se sube. Cada
 * cambio pasa por ajustarStock con su motivo (Kardex, regla de negocio 2) y
 * con un solo error no se cambia nada.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanillaStockService - cantidades desde Excel")
class PlanillaStockServiceTest {

    @Mock private ProductoService productoService;
    @Mock private ProductoRepository productoRepository;

    @InjectMocks
    private PlanillaStockService planilla;

    private static Producto producto(long id, String codigo, String nombre, int stock) {
        Producto p = new Producto();
        p.setId(id);
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setTipoProducto(TipoProducto.LLAVE_VIRGEN);
        p.setCantidadStock(stock);
        p.setActivo(true);
        p.setPrecioCompra(new java.math.BigDecimal("0.90"));
        p.setPrecioVenta(new java.math.BigDecimal("1.50"));
        return p;
    }

    private static byte[] utf8(String csv) {
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("La descarga abre en columnas en Excel y protege el código de volverse número o fecha")
    void exportar() {
        Producto llave = producto(1, "001", "Llave de diseño", 250);
        llave.setMarca("Cantol; S.A.");
        when(productoRepository.findByActivoTrueOrderByTipoProductoAscNombreAsc()).thenReturn(List.of(llave));

        String csv = new String(planilla.exportar(), Charset.forName("windows-1252"));

        assertThat(csv).startsWith(
            "sep=;\r\ncodigo;nombre;tipo;marca;stock_actual;cantidad;precio_compra;precio_venta;precio_duplicado\r\n");
        assertThat(csv).contains("=\"001\";Llave de diseño;llave virgen;\"Cantol; S.A.\";250;;=\"0.90\";=\"1.50\";\r\n");
    }

    @Test
    @DisplayName("Lo descargado se puede volver a subir tal cual (con ;, ANSI y el código como fórmula)")
    void idaYVuelta() {
        Producto llave = producto(1, "001", "Llave de diseño", 250);
        when(productoRepository.findByActivoTrueOrderByTipoProductoAscNombreAsc()).thenReturn(List.of(llave));
        String llenado = new String(planilla.exportar(), Charset.forName("windows-1252")).replace("250;;", "250;300;");
        when(productoRepository.findByCodigoIn(anyCollection())).thenReturn(List.of(llave));

        ResultadoPlanilla r = planilla.actualizar(
            llenado.getBytes(Charset.forName("windows-1252")), PlanillaStockService.Modo.CONTEO, false);

        assertThat(r.errores()).isEmpty();
        assertThat(r.cambios()).singleElement().satisfies(c -> {
            assertThat(c.codigo()).isEqualTo("001");
            assertThat(c.stockNuevo()).isEqualTo(300);
        });
    }

    @Test
    @DisplayName("Conteo: el número reemplaza el stock; sin cambio no genera movimiento")
    void conteo_reemplaza() {
        when(productoRepository.findByCodigoIn(anyCollection())).thenReturn(List.of(
            producto(1, "KW1", "Llave KW1", 200), producto(2, "YA1", "Llave YA1", 40)));

        ResultadoPlanilla r = planilla.actualizar(
            utf8("codigo,cantidad\nKW1,150\nYA1,40\nSIN,\n"), PlanillaStockService.Modo.CONTEO, true);

        assertThat(r.errores()).isEmpty();
        assertThat(r.filasLeidas()).isEqualTo(2);
        assertThat(r.sinCambio()).isEqualTo(1);
        assertThat(r.actualizados()).isEqualTo(1);
        verify(productoService).ajustarStock(1L, new AjusteStockRequest(-50, PlanillaStockService.MOTIVO_CONTEO));
        verify(productoService, never()).ajustarStock(2L, new AjusteStockRequest(0, PlanillaStockService.MOTIVO_CONTEO));
    }

    @Test
    @DisplayName("Entrada: el número se suma, y 1,000 es mil (no uno)")
    void entrada_suma() {
        when(productoRepository.findByCodigoIn(anyCollection())).thenReturn(List.of(producto(1, "KW1", "Llave KW1", 200)));

        ResultadoPlanilla r = planilla.actualizar(
            utf8("codigo;cantidad\nKW1;\"1,000\"\n"), PlanillaStockService.Modo.ENTRADA, true);

        assertThat(r.errores()).isEmpty();
        assertThat(r.cambios().getFirst().stockNuevo()).isEqualTo(1200);
        verify(productoService).ajustarStock(1L, new AjusteStockRequest(1000, PlanillaStockService.MOTIVO_ENTRADA));
    }

    @Test
    @DisplayName("Con un error no se cambia ningún stock")
    void conError_noCambiaNada() {
        when(productoRepository.findByCodigoIn(anyCollection())).thenReturn(List.of(producto(1, "KW1", "Llave KW1", 200)));

        ResultadoPlanilla r = planilla.actualizar(
            utf8("codigo,cantidad\nKW1,300\nNOEXISTE,5\nKW1,2\n"), PlanillaStockService.Modo.CONTEO, true);

        assertThat(r.errores()).extracting(e -> e.fila()).containsExactly(3, 4);
        assertThat(r.actualizados()).isZero();
        verify(productoService, never()).ajustarStock(anyLong(), any());
    }

    @Test
    @DisplayName("Cantidades inválidas: negativas o con decimales")
    void cantidadesInvalidas() {
        assertThat(PlanillaStockService.cantidad("-5")).isNull();
        assertThat(PlanillaStockService.cantidad("2.5")).isNull();
        assertThat(PlanillaStockService.cantidad("1.000")).isEqualTo(1000);
        assertThat(PlanillaStockService.cantidad(" 120 ")).isEqualTo(120);
    }

    @Test
    @DisplayName("Precios: cambia solo lo escrito y distinto; el duplicado solo en llaves")
    void precios() {
        Producto llave = producto(1, "CASA-1", "Llave casa", 300);
        llave.setPrecioDuplicado(new java.math.BigDecimal("2.00"));
        Producto candado = producto(2, "CAN-1", "Candado", 5);
        candado.setTipoProducto(TipoProducto.CANDADO);
        when(productoRepository.findByCodigoIn(anyCollection())).thenReturn(List.of(llave, candado));

        ResultadoPlanilla r = planilla.actualizar(
            utf8("codigo;cantidad;precio_compra;precio_duplicado\nCASA-1;;0,90;2,50\nCAN-1;;;\n"),
            PlanillaStockService.Modo.CONTEO, true);

        assertThat(r.errores()).isEmpty();
        assertThat(r.cambios()).singleElement().satisfies(c -> {
            assertThat(c.stockNuevo()).as("sin cantidad no toca el stock").isNull();
            assertThat(c.precios()).singleElement().satisfies(p -> {
                assertThat(p.precio()).isEqualTo("duplicado");
                assertThat(p.nuevo()).isEqualByComparingTo("2.50");
            });
        });
        verify(productoService).actualizarPrecios(1L, null, null, new java.math.BigDecimal("2.50"), "Planilla de Excel");
        verify(productoService, never()).ajustarStock(anyLong(), any());

        ResultadoPlanilla conError = planilla.actualizar(
            utf8("codigo;precio_duplicado\nCAN-1;2.50\n"), PlanillaStockService.Modo.CONTEO, false);
        assertThat(conError.errores()).singleElement().satisfies(e -> assertThat(e.mensaje()).contains("no es una llave"));
    }
}
