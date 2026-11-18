package com.cerrajeria.sig.inventario;

import com.cerrajeria.sig.common.excel.EstiloExcel;
import com.cerrajeria.sig.common.excel.LibroXlsx;
import lombok.RequiredArgsConstructor;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Los dos Excel del inventario, con formato y una hoja de instrucciones:
 * <ul>
 *   <li>la <b>plantilla</b> para cargar productos nuevos (Inventario → Importar), con lista
 *       desplegable de tipos y validaciones, y</li>
 *   <li>la <b>planilla</b> con el inventario de hoy y la columna "cantidad" resaltada, para contar o
 *       sumar lo que llego y corregir precios.</li>
 * </ul>
 * Los nombres de las columnas son los que entienden los importadores; ambos aceptan
 * el .xlsx tal cual, sin guardarlo como CSV.
 */
@Service
@RequiredArgsConstructor
public class PlantillasInventarioService {

    /** Filas con listas y validaciones: el maximo que acepta una importacion. */
    private static final int FILAS = ImportacionProductosService.MAX_FILAS;

    private static final Map<TipoProducto, String> ETIQUETA = Map.ofEntries(
        Map.entry(TipoProducto.LLAVE_VIRGEN, "Llave virgen"), Map.entry(TipoProducto.LLAVE_COLOR, "Llave de color"),
        Map.entry(TipoProducto.LLAVE_DISENO, "Llave de diseño"), Map.entry(TipoProducto.CHAPA, "Chapa"),
        Map.entry(TipoProducto.CILINDRO, "Cilindro"), Map.entry(TipoProducto.CANDADO, "Candado"),
        Map.entry(TipoProducto.LLAVERO, "Llavero"), Map.entry(TipoProducto.ARGOLLA, "Argolla"),
        Map.entry(TipoProducto.ACCESORIO, "Accesorio"),
        Map.entry(TipoProducto.REPUESTO_LICUADORA, "Repuesto de licuadora"));

    private record Col(String titulo, boolean obligatoria, double ancho, String formato, String queEs, String ejemplo) {
    }

    private static final List<Col> COLUMNAS = List.of(
        new Col("Código *", true, 18, "@", "El código del producto. No se puede repetir ni usar uno que ya exista.", "KW1-CV"),
        new Col("Nombre *", true, 34, "@", "Como se llama el producto.", "Llave KW1 CV"),
        new Col("Tipo *", true, 24, "@", "Elige de la lista desplegable.", "Llave virgen"),
        new Col("Marca", false, 16, "@", "Opcional.", "CV"),
        new Col("Stock inicial", false, 15, "0", "Cuántas unidades hay hoy. Vacío = 0. Queda en el Kárdex.", "250"),
        new Col("Stock mínimo", false, 15, "0", "Con cuántas se avisa que se acaba. Vacío = 5.", "50"),
        new Col("Precio compra *", true, 16, "0.00", "Lo que te cuesta a ti, en soles.", "0.90"),
        new Col("Precio venta *", true, 16, "0.00", "A cuánto se vende, en soles. En una llave, la llave sola.", "2.50"),
        new Col("Precio duplicado", false, 17, "0.00", "Solo llaves: lo que cobras por el duplicado. Vacío = no aparece en Duplicado de llave.", "2.50"),
        new Col("Perfil", false, 14, "@", "Solo llaves: el perfil o modelo de la llave.", "KW1"),
        new Col("Color", false, 14, "@", "Solo llaves de color.", ""));

    private final ProductoRepository productoRepository;

    // ------------------------------------------------------------------ plantilla de importacion

    public byte[] plantillaImportacion() {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            Workbook libro = new Workbook(salida, "SIG Cerrajeria", "1.0");
            Worksheet datos = libro.newWorksheet("Productos");
            datos.setTabColor(EstiloExcel.AZUL);
            for (int c = 0; c < COLUMNAS.size(); c++) {
                Col col = COLUMNAS.get(c);
                datos.value(0, c, col.titulo());
                datos.width(c, col.ancho());
                datos.style(c).format(col.formato()).set();
            }
            // Oscura = obligatoria, clara = opcional
            for (int c = 0; c < COLUMNAS.size(); c++) {
                EstiloExcel.cabecera(datos, 0, c, c, COLUMNAS.get(c).obligatoria() ? EstiloExcel.AZUL : EstiloExcel.AZUL_SUAVE);
            }
            datos.freezePane(0, 1);

            String tipos = ETIQUETA.values().stream().sorted().collect(Collectors.joining(","));
            datos.range(1, 2, FILAS, 2).validateWithListByFormula("\"" + tipos + "\"")
                .showDropdown(true).allowBlank(false).showErrorMessage(true)
                .errorTitle("Tipo no válido").error("Elige un tipo de la lista.");
            datos.range(1, 4, FILAS, 5).validateWithFormula("OR(E2=\"\",AND(ISNUMBER(E2),E2>=0,E2=INT(E2)))")
                .showErrorMessage(true).errorTitle("Número entero").error("Escribe un número entero, sin signo (por ejemplo 25).");
            datos.range(1, 6, FILAS, 8).validateWithFormula("OR(G2=\"\",AND(ISNUMBER(G2),G2>=0))")
                .showErrorMessage(true).errorTitle("Precio").error("Escribe un precio en soles, por ejemplo 2.50.");
            EstiloExcel.imprimible(datos, true, "Plantilla de productos");

            instruccionesImportacion(libro.newWorksheet("Instrucciones"));
            libro.finish();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo armar la plantilla", ex);
        }
        return LibroXlsx.normalizar(salida.toByteArray());
    }

    private static void instruccionesImportacion(Worksheet h) {
        h.setTabColor(EstiloExcel.AMARILLO);
        h.hideGridLines();
        h.width(0, 22);
        h.width(1, 16);
        h.width(2, 70);
        h.width(3, 16);
        EstiloExcel.titulo(h, 0, "Cómo cargar productos con esta plantilla");
        int f = 2;
        String[] pasos = {
            "1. Ve a la hoja «Productos» (la primera pestaña) y escribe un producto por fila, empezando en la fila 2.",
            "2. Las columnas con la cabecera azul oscura son obligatorias; las de azul claro son opcionales.",
            "3. Guarda el archivo (Ctrl + S) como .xlsx. No hace falta convertirlo a CSV.",
            "4. En el sistema: Inventario → Importar, y elige el archivo. Primero te muestra lo que va a cargar y no guarda nada hasta que confirmes.",
        };
        for (String paso : pasos) {
            h.value(f++, 0, paso);
        }
        f++;
        EstiloExcel.seccion(h, f++, "Las columnas");
        int cabecera = f++;
        h.value(cabecera, 0, "Columna");
        h.value(cabecera, 1, "¿Obligatoria?");
        h.value(cabecera, 2, "Qué poner");
        h.value(cabecera, 3, "Ejemplo");
        EstiloExcel.cabecera(h, cabecera, 0, 3, EstiloExcel.AZUL);
        for (Col col : COLUMNAS) {
            h.value(f, 0, col.titulo().replace(" *", ""));
            h.value(f, 1, col.obligatoria() ? "Sí" : "No");
            h.value(f, 2, col.queEs());
            h.value(f, 3, col.ejemplo());
            f++;
        }
        EstiloExcel.bordes(h, cabecera, 0, f - 1, 3);
        h.range(cabecera + 1, 0, f - 1, 3).style().wrapText(true).verticalAlignment("center").set();
        f++;
        EstiloExcel.seccion(h, f++, "Ten en cuenta");
        String[] avisos = {
            "• Solo se crean productos nuevos. Para cambiar el stock o el precio de uno que ya existe, usa la planilla de cantidades (Inventario → Cantidades y precios con Excel).",
            "• Todo o nada: si una sola fila tiene un error, no se carga ninguna y el sistema te dice cuál es.",
            "• El stock inicial queda registrado en el Kárdex del producto.",
            "• Los precios pueden llevar punto o coma (2.50 o 2,50). No hace falta escribir S/.",
            "• Hasta " + FILAS + " productos por carga.",
        };
        for (String aviso : avisos) {
            h.value(f++, 0, aviso);
        }
        f++;
        EstiloExcel.seccion(h, f++, "Tipos de producto");
        for (String tipo : ETIQUETA.values().stream().sorted().toList()) {
            h.value(f++, 0, tipo);
        }
        EstiloExcel.imprimible(h, false, "Instrucciones");
    }

    // ------------------------------------------------------------------ planilla de cantidades y precios

    /** Todo el catalogo activo: "cantidad" vacia y resaltada, los precios de hoy para corregir. */
    @Transactional(readOnly = true)
    public byte[] planillaCantidades() {
        List<Producto> productos = productoRepository.findByActivoTrueOrderByTipoProductoAscNombreAsc();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            Workbook libro = new Workbook(salida, "SIG Cerrajeria", "1.0");
            Worksheet h = libro.newWorksheet("Inventario");
            h.setTabColor(EstiloExcel.AZUL);
            String[] titulos = {"Código", "Nombre", "Tipo", "Marca", "Stock actual", "Cantidad",
                "Precio compra", "Precio venta", "Precio duplicado"};
            double[] anchos = {20, 36, 22, 16, 14, 14, 15, 15, 17};
            for (int c = 0; c < titulos.length; c++) {
                h.value(0, c, titulos[c]);
                h.width(c, anchos[c]);
            }
            EstiloExcel.cabecera(h, 0, 0, 8, EstiloExcel.AZUL);
            EstiloExcel.cabecera(h, 0, 5, 5, "B7791F"); // donde se escribe: destaca
            h.freezePane(1, 1);

            int fila = 1;
            for (Producto p : productos) {
                h.value(fila, 0, p.getCodigo());
                h.value(fila, 1, p.getNombre());
                h.value(fila, 2, ETIQUETA.getOrDefault(p.getTipoProducto(), p.getTipoProducto().name()));
                if (p.getMarca() != null) h.value(fila, 3, p.getMarca());
                h.value(fila, 4, p.getCantidadStock());
                dinero(h, fila, 6, p.getPrecioCompra());
                dinero(h, fila, 7, p.getPrecioVenta());
                dinero(h, fila, 8, p.getPrecioDuplicado());
                fila++;
            }
            int ultima = Math.max(fila - 1, 1);
            h.range(1, 0, ultima, 4).style().fillColor(EstiloExcel.GRIS).fontColor(EstiloExcel.SUAVE).set(); // solo lectura
            h.range(1, 5, ultima, 5).style().fillColor(EstiloExcel.AMARILLO).bold().format("0").set();       // aqui se escribe
            h.range(1, 6, ultima, 8).style().format("0.00").set();
            EstiloExcel.bordes(h, 0, 0, ultima, 8);
            h.range(1, 5, FILAS, 5).validateWithFormula("OR(F2=\"\",AND(ISNUMBER(F2),F2>=0,F2=INT(F2)))")
                .showErrorMessage(true).errorTitle("Cantidad").error("Escribe un número entero sin signo (por ejemplo 850).");
            h.range(1, 6, FILAS, 8).validateWithFormula("OR(G2=\"\",AND(ISNUMBER(G2),G2>=0))")
                .showErrorMessage(true).errorTitle("Precio").error("Escribe un precio en soles, por ejemplo 2.50.");
            if (!productos.isEmpty()) {
                h.range(0, 0, ultima, 8).createTable("Código", "Nombre", "Tipo", "Marca", "Stock actual", "Cantidad",
                    "Precio compra", "Precio venta", "Precio duplicado").setName("Inventario").setDisplayName("Inventario");
            }
            EstiloExcel.imprimible(h, true, "Inventario");
            h.repeatRows(0);

            instruccionesPlanilla(libro.newWorksheet("Instrucciones"));
            libro.finish();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo armar la planilla", ex);
        }
        return LibroXlsx.normalizar(salida.toByteArray());
    }

    private static void dinero(Worksheet h, int fila, int col, BigDecimal valor) {
        if (valor != null) {
            h.value(fila, col, valor);
        }
    }

    private static void instruccionesPlanilla(Worksheet h) {
        h.setTabColor(EstiloExcel.AMARILLO);
        h.hideGridLines();
        h.width(0, 110);
        EstiloExcel.titulo(h, 0, "Cómo usar la planilla de cantidades y precios");
        String[] lineas = {
            "",
            "1. En la hoja «Inventario», escribe en la columna amarilla «Cantidad» solo lo que quieras cambiar. Las filas que dejes vacías no se tocan.",
            "2. Para corregir un precio, cámbialo en «Precio compra», «Precio venta» o «Precio duplicado» (este último solo en llaves).",
            "3. Las columnas grises (código, nombre, tipo, marca y stock actual) son de solo lectura: cambiarlas no hace nada.",
            "4. Guarda el archivo (Ctrl + S) como .xlsx.",
            "5. En el sistema: Inventario → Cantidades y precios con Excel. Elige el archivo y, antes de confirmar, indica qué significa el número:",
            "      • «Lo que conté» (reemplaza): hay 200 y escribes 850 → el stock queda en 850. Úsalo después de contar el local.",
            "      • «Lo que llegó» (se suma): hay 200 y escribes 1000 → el stock queda en 1200. Para registrar el gasto, usa Compras.",
            "6. Primero ves todos los cambios y nada se guarda hasta que confirmes. Cada cambio de stock queda en el Kárdex con su motivo.",
            "",
            "Si una fila tiene un error, no se aplica ninguna y el sistema te dice cuál es.",
        };
        int f = 1;
        for (String linea : lineas) {
            h.value(f++, 0, linea);
        }
        h.range(1, 0, f, 0).style().wrapText(true).verticalAlignment("top").set();
        EstiloExcel.imprimible(h, false, "Instrucciones");
    }
}
