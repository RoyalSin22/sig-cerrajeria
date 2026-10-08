package com.cerrajeria.sig.inventario;

import com.cerrajeria.sig.inventario.dto.ImportacionResultado;
import com.cerrajeria.sig.inventario.dto.ProductoCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Importacion de inventario desde CSV: el archivo viene de Excel (separador y
 * codificacion segun la PC) y la carga es todo o nada, con el stock inicial
 * pasando por ProductoService.crear (Kardex, regla de negocio 2).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImportacionProductosService - importar inventario desde CSV")
class ImportacionProductosServiceTest {

    @Mock private ProductoService productoService;
    @Mock private ProductoRepository productoRepository;

    @InjectMocks
    private ImportacionProductosService importacion;

    private static final String CABECERA = "codigo,nombre,tipo,marca,stock,stock_minimo,precio_compra,precio_venta\n";

    private static byte[] utf8(String csv) {
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Confirmado y sin errores: crea cada producto con el motivo de importación (Kárdex)")
    void creaProductos_porElCaminoDelKardex() {
        String csv = CABECERA
            + "KW1-001,Llave virgen KW1,llave virgen,Kwikset,50,10,1.20,3.00\n"
            + "CAN-40,Candado 40 mm,Candado,,3,,15,25.50\n";

        ImportacionResultado r = importacion.importar(utf8(csv), true);

        assertThat(r.errores()).isEmpty();
        assertThat(r.creados()).isEqualTo(2);
        ArgumentCaptor<ProductoCreateRequest> captor = ArgumentCaptor.forClass(ProductoCreateRequest.class);
        verify(productoService, times(2)).crear(captor.capture(), eq(ImportacionProductosService.MOTIVO_STOCK_IMPORTADO));
        ProductoCreateRequest llave = captor.getAllValues().get(0);
        assertThat(llave.tipoProducto()).isEqualTo(TipoProducto.LLAVE_VIRGEN);
        assertThat(llave.cantidadStock()).isEqualTo(50);
        assertThat(llave.precioVenta()).isEqualByComparingTo("3.00");
        ProductoCreateRequest candado = captor.getAllValues().get(1);
        assertThat(candado.marca()).isNull();
        assertThat(candado.stockMinimo()).as("vacío: se usa el mínimo por defecto").isNull();
        assertThat(candado.precioVenta()).isEqualByComparingTo("25.50");
    }

    @Test
    @DisplayName("Sin confirmar es solo una revisión: no crea nada")
    void revision_noCreaNada() {
        ImportacionResultado r = importacion.importar(utf8(CABECERA + "A1,Llave,llave_virgen,,1,,1,2\n"), false);

        assertThat(r.errores()).isEmpty();
        assertThat(r.productos()).hasSize(1);
        assertThat(r.creados()).isZero();
        verify(productoService, never()).crear(any(), anyString());
    }

    @Test
    @DisplayName("Con un solo error no se crea ningún producto, y se informa la fila como en Excel")
    void todoONada_conErrores() {
        String csv = CABECERA
            + "A1,Llave buena,llave_virgen,,1,,1,2\n"
            + "A2,,chapa,,1,,1,2\n"                     // fila 3: falta nombre
            + "A3,Otra,martillo,,-2,,x,2\n";            // fila 4: tipo, stock y precio inválidos

        ImportacionResultado r = importacion.importar(utf8(csv), true);

        assertThat(r.creados()).isZero();
        verify(productoService, never()).crear(any(), anyString());
        assertThat(r.errores()).extracting(ImportacionResultado.ErrorFila::fila).containsExactly(3, 4, 4, 4);
        assertThat(r.errores().get(0).mensaje()).contains("nombre");
        assertThat(r.errores().get(1).mensaje()).contains("Tipo desconocido \"martillo\"");
        assertThat(r.productos()).extracting(ImportacionResultado.FilaProducto::codigo).containsExactly("A1");
    }

    @Test
    @DisplayName("Rechaza códigos repetidos en el archivo y códigos que ya existen en el inventario")
    void codigosRepetidosOExistentes() {
        when(productoRepository.findCodigosExistentes(any())).thenReturn(List.of("YA-EXISTE"));
        String csv = CABECERA
            + "A1,Uno,chapa,,1,,1,2\n"
            + "A1,Dos,chapa,,1,,1,2\n"
            + "YA-EXISTE,Tres,chapa,,1,,1,2\n";

        ImportacionResultado r = importacion.importar(utf8(csv), true);

        assertThat(r.creados()).isZero();
        assertThat(r.errores()).hasSize(2);
        assertThat(r.errores().get(0).fila()).isEqualTo(3);
        assertThat(r.errores().get(0).mensaje()).contains("repetido").contains("fila 2");
        assertThat(r.errores().get(1).fila()).isEqualTo(4);
        assertThat(r.errores().get(1).mensaje()).contains("ya existe");
    }

    @Test
    @DisplayName("Lee el CSV de Excel en español: punto y coma, coma decimal, Windows-1252, S/ y cabeceras con tildes")
    void csvDeExcelEnEspanol() {
        String csv = "Código;Nombre;Tipo;Stock;Stock mínimo;Precio de compra;Precio de venta;Tamaño\r\n"
            + "LLD-01;Llave de diseño Perú;Llave de diseño;2;1;S/ 4,50;S/. 12,5;Grande\r\n"
            + ";;;;;;;\r\n";   // fila vacía al final, típica de Excel

        ImportacionResultado r = importacion.importar(csv.getBytes(Charset.forName("windows-1252")), true);

        assertThat(r.errores()).isEmpty();
        assertThat(r.columnasIgnoradas()).isEmpty();
        assertThat(r.filasLeidas()).isEqualTo(1);
        ArgumentCaptor<ProductoCreateRequest> captor = ArgumentCaptor.forClass(ProductoCreateRequest.class);
        verify(productoService).crear(captor.capture(), anyString());
        ProductoCreateRequest req = captor.getValue();
        assertThat(req.nombre()).isEqualTo("Llave de diseño Perú");
        assertThat(req.tipoProducto()).isEqualTo(TipoProducto.LLAVE_DISENO);
        assertThat(req.precioCompra()).isEqualByComparingTo("4.50");
        assertThat(req.precioVenta()).isEqualByComparingTo("12.50");
        assertThat(req.productoTamano()).isEqualTo("Grande");
    }

    @Test
    @DisplayName("Lee UTF-8 con BOM, la línea sep= de la plantilla y campos entre comillas")
    void utf8ConBomSepYComillas() {
        String csv = "﻿sep=;\ncodigo;nombre;tipo;precio_compra;precio_venta;notas\n"
            + "CH-1;\"Chapa \"\"Forte\"\"; 3 golpes\";chapa;80;120;cualquier cosa\n";

        ImportacionResultado r = importacion.importar(utf8(csv), false);

        assertThat(r.errores()).isEmpty();
        assertThat(r.productos()).singleElement().satisfies(p -> {
            assertThat(p.nombre()).isEqualTo("Chapa \"Forte\"; 3 golpes");
            assertThat(p.fila()).as("sep= no se ve en Excel: la cabecera es la fila 1").isEqualTo(2);
            assertThat(p.cantidadStock()).as("sin columna stock: 0").isZero();
        });
        assertThat(r.columnasIgnoradas()).containsExactly("notas");
    }

    @Test
    @DisplayName("Falta una columna obligatoria: error de cabecera, sin revisar filas")
    void faltaColumnaObligatoria() {
        ImportacionResultado r = importacion.importar(utf8("codigo,nombre,tipo,precio_venta\nA,B,chapa,1\n"), true);

        assertThat(r.errores()).singleElement()
            .satisfies(e -> assertThat(e.mensaje()).contains("precio_compra"));
        verify(productoService, never()).crear(any(), anyString());
    }

    @Test
    @DisplayName("Un libro .xlsx se rechaza con instrucciones para guardarlo como CSV")
    void rechazaXlsx() {
        byte[] zip = {'P', 'K', 3, 4, 0, 0};
        assertThatThrownBy(() -> importacion.importar(zip, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CSV");
    }

    @Test
    @DisplayName("Formatos de número que produce Excel")
    void formatosDeNumero() {
        assertThat(ImportacionProductosService.numero("12.50")).isEqualByComparingTo("12.50");
        assertThat(ImportacionProductosService.numero("12,5")).isEqualByComparingTo("12.5");
        assertThat(ImportacionProductosService.numero("1,250.00")).isEqualByComparingTo("1250");
        assertThat(ImportacionProductosService.numero("1.250,00")).isEqualByComparingTo("1250");
        assertThat(ImportacionProductosService.numero("S/. 3")).isEqualByComparingTo("3");
        assertThat(ImportacionProductosService.numero("abc")).isNull();
        assertThat(ImportacionProductosService.numero("10.0")).isEqualByComparingTo(BigDecimal.TEN);
    }
}
