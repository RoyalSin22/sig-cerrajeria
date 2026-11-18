package com.cerrajeria.sig.analisis;

import com.cerrajeria.sig.common.excel.LibroXlsx;
import lombok.RequiredArgsConstructor;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Un libro de Excel para Power BI: una hoja (y una tabla de Excel con nombre)
 * por tema, con fechas y montos como tipos de verdad, no como texto. Sigue el
 * esquema en estrella: Ventas, Cobros, Taller, etc. son hechos con su fecha;
 * Productos es la dimension, con el codigo como clave para relacionarlas.
 *
 * Se consulta con SQL directo (solo lectura) en vez de entidades: son reportes
 * planos de toda la base, y asi cada fila se escribe apenas se lee.
 */
@Service
@RequiredArgsConstructor
public class ExportacionPowerBiService {

    private final JdbcTemplate jdbc;

    enum Tipo { TEXTO, ETIQUETA, ENTERO, DINERO, FECHA, FECHA_HORA, HORA, SI_NO }

    /** {@code columna} es el alias en el SELECT. */
    record Col(String titulo, Tipo tipo, String columna) {
    }

    /** Codigos de la base como se leen en la app (tipos de producto, metodos de pago, estados). */
    private static final Map<String, String> ETIQUETAS = Map.ofEntries(
        Map.entry("LLAVE_VIRGEN", "Llave virgen"), Map.entry("LLAVE_COLOR", "Llave de color"),
        Map.entry("LLAVE_DISENO", "Llave de diseño"), Map.entry("REPUESTO_LICUADORA", "Repuesto de licuadora"),
        Map.entry("EFECTIVO", "Efectivo"), Map.entry("YAPE", "Yape"), Map.entry("PLIN", "Plin"),
        Map.entry("TARJETA", "Tarjeta"), Map.entry("MIXTO", "Mixto"),
        Map.entry("EN_REPARACION", "En reparación"), Map.entry("ESPERANDO_REPUESTO", "Esperando repuesto"),
        Map.entry("EN_CAMINO", "En camino"), Map.entry("ORDEN_TRABAJO", "Orden de trabajo"));

    @Transactional(readOnly = true)
    public byte[] exportar(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
        Timestamp inicio = Timestamp.valueOf(desde.atStartOfDay());
        Timestamp fin = Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()); // excluido
        Date dDesde = Date.valueOf(desde);
        Date dHasta = Date.valueOf(hasta);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            Workbook libro = new Workbook(salida, "SIG Cerrajeria", "1.0");
            ventas(libro, inicio, fin, dDesde, dHasta);
            cobros(libro, inicio, fin);
            taller(libro, inicio, fin);
            domicilio(libro, dDesde, dHasta);
            productos(libro);
            kardex(libro, inicio, fin);
            compras(libro, dDesde, dHasta);
            cajas(libro, inicio, fin);
            movimientosCaja(libro, inicio, fin);
            leeme(libro, desde, hasta);
            libro.finish();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo armar el Excel", ex);
        }
        return LibroXlsx.normalizar(salida.toByteArray());
    }

    /** Una fila por linea vendida: del sistema y, aparte, las ventas anteriores cargadas desde archivo. */
    private void ventas(Workbook libro, Timestamp inicio, Timestamp fin, Date desde, Date hasta) {
        hoja(libro, "Ventas", List.of(
                new Col("Fecha", Tipo.FECHA, "fecha"), new Col("Hora", Tipo.HORA, "fecha_hora"),
                new Col("Fecha y hora", Tipo.FECHA_HORA, "fecha_hora"), new Col("Origen", Tipo.TEXTO, "origen"),
                new Col("Venta", Tipo.ENTERO, "venta"), new Col("Código", Tipo.TEXTO, "codigo"),
                new Col("Producto", Tipo.TEXTO, "producto"), new Col("Tipo", Tipo.ETIQUETA, "tipo"),
                new Col("Marca", Tipo.TEXTO, "marca"), new Col("Línea", Tipo.TEXTO, "linea"),
                new Col("Cantidad", Tipo.ENTERO, "cantidad"), new Col("Precio unitario", Tipo.DINERO, "precio"),
                new Col("Subtotal", Tipo.DINERO, "subtotal"), new Col("Costo unitario", Tipo.DINERO, "costo"),
                new Col("Método de pago", Tipo.ETIQUETA, "metodo"), new Col("Vendedor", Tipo.TEXTO, "vendedor"),
                new Col("Cliente", Tipo.TEXTO, "cliente"), new Col("Anulada", Tipo.SI_NO, "anulada")),
            """
            SELECT CAST(v.fecha_hora AS DATE) AS fecha, v.fecha_hora, 'Sistema' AS origen, v.id AS venta,
                   p.codigo, COALESCE(p.nombre, d.descripcion) AS producto, p.tipo_producto AS tipo, p.marca,
                   CASE WHEN d.producto_id IS NULL THEN 'Servicio' WHEN d.es_duplicado THEN 'Duplicado'
                        ELSE 'Producto' END AS linea,
                   d.cantidad, d.precio_unitario AS precio, d.subtotal, p.precio_compra AS costo,
                   v.metodo_pago AS metodo, u.nombre AS vendedor, c.nombre AS cliente, v.anulada
            FROM detalle_ventas d
            JOIN ventas v ON v.id = d.venta_id
            JOIN usuarios u ON u.id = v.usuario_id
            LEFT JOIN productos p ON p.id = d.producto_id
            LEFT JOIN clientes c ON c.id = v.cliente_id
            WHERE v.fecha_hora >= ? AND v.fecha_hora < ?
            UNION ALL
            SELECT a.fecha, NULL, 'Anterior', NULL, p.codigo, COALESCE(p.nombre, a.descripcion), p.tipo_producto,
                   p.marca, CASE WHEN a.producto_id IS NULL THEN 'Sin producto' ELSE 'Producto' END,
                   a.cantidad, a.precio_unitario, a.cantidad * a.precio_unitario,
                   p.precio_compra, a.metodo_pago, NULL, NULL, FALSE
            FROM ventas_anteriores a
            LEFT JOIN productos p ON p.id = a.producto_id
            WHERE a.fecha BETWEEN ? AND ?
            ORDER BY 1, 2 NULLS FIRST, 4
            """, inicio, fin, desde, hasta);
    }

    /**
     * Todo el dinero que entro (o salio, en negativo) por metodo de pago: ventas
     * (un pago mixto, linea por linea), cobros del taller y de domicilio.
     */
    private void cobros(Workbook libro, Timestamp inicio, Timestamp fin) {
        hoja(libro, "Cobros", List.of(
                new Col("Fecha", Tipo.FECHA, "fecha"), new Col("Hora", Tipo.HORA, "fecha_hora"),
                new Col("Fecha y hora", Tipo.FECHA_HORA, "fecha_hora"), new Col("Origen", Tipo.TEXTO, "origen"),
                new Col("Referencia", Tipo.TEXTO, "referencia"), new Col("Concepto", Tipo.TEXTO, "concepto"),
                new Col("Método de pago", Tipo.ETIQUETA, "metodo"), new Col("Monto", Tipo.DINERO, "monto"),
                new Col("Usuario", Tipo.TEXTO, "usuario"), new Col("Anulada", Tipo.SI_NO, "anulada")),
            """
            SELECT CAST(v.fecha_hora AS DATE) AS fecha, v.fecha_hora, 'Venta' AS origen,
                   'V-' || v.id AS referencia, 'Venta' AS concepto,
                   COALESCE(pv.metodo_pago, v.metodo_pago) AS metodo, COALESCE(pv.monto, v.monto_total) AS monto,
                   u.nombre AS usuario, v.anulada
            FROM ventas v
            JOIN usuarios u ON u.id = v.usuario_id
            LEFT JOIN pagos_venta pv ON pv.venta_id = v.id AND v.metodo_pago = 'MIXTO'
            WHERE v.fecha_hora >= ? AND v.fecha_hora < ?
            UNION ALL
            SELECT CAST(po.fecha_hora AS DATE), po.fecha_hora, 'Taller', o.numero_orden,
                   CASE po.tipo_pago WHEN 'ADELANTO' THEN 'Adelanto' WHEN 'LIQUIDACION' THEN 'Liquidación'
                        ELSE 'Devolución' END,
                   po.metodo_pago, CASE WHEN po.tipo_pago = 'DEVOLUCION' THEN -po.monto ELSE po.monto END,
                   u.nombre, FALSE
            FROM pagos_orden po
            JOIN ordenes_trabajo o ON o.id = po.orden_trabajo_id
            JOIN usuarios u ON u.id = po.usuario_id
            WHERE po.fecha_hora >= ? AND po.fecha_hora < ?
            UNION ALL
            SELECT CAST(s.fecha_realizado AS DATE), s.fecha_realizado, 'Domicilio', 'D-' || s.id, s.tipo_servicio,
                   s.metodo_pago, s.monto_cobrado, u.nombre, FALSE
            FROM servicios_domicilio s
            JOIN usuarios u ON u.id = s.realizado_por
            WHERE s.estado = 'REALIZADO' AND s.monto_cobrado > 0
              AND s.fecha_realizado >= ? AND s.fecha_realizado < ?
            ORDER BY 2
            """, inicio, fin, inicio, fin, inicio, fin);
    }

    private void taller(Workbook libro, Timestamp inicio, Timestamp fin) {
        hoja(libro, "Taller", List.of(
                new Col("Orden", Tipo.TEXTO, "numero_orden"), new Col("Fecha ingreso", Tipo.FECHA, "ingreso"),
                new Col("Fecha entrega", Tipo.FECHA, "entrega"), new Col("Estado", Tipo.ETIQUETA, "estado"),
                new Col("Cliente", Tipo.TEXTO, "cliente"), new Col("Trabajo", Tipo.TEXTO, "producto_recibido"),
                new Col("Precio total", Tipo.DINERO, "precio_total"), new Col("Pagado", Tipo.DINERO, "monto_pagado"),
                new Col("Saldo", Tipo.DINERO, "saldo_pendiente"), new Col("Garantía", Tipo.SI_NO, "es_garantia"),
                new Col("Días en el taller", Tipo.ENTERO, "dias"), new Col("Registró", Tipo.TEXTO, "usuario")),
            """
            SELECT o.numero_orden, CAST(o.fecha_ingreso AS DATE) AS ingreso, CAST(o.fecha_entrega AS DATE) AS entrega,
                   o.estado, c.nombre AS cliente, o.producto_recibido, o.precio_total, o.monto_pagado,
                   o.saldo_pendiente, o.es_garantia,
                   CAST(o.fecha_entrega AS DATE) - CAST(o.fecha_ingreso AS DATE) AS dias, u.nombre AS usuario
            FROM ordenes_trabajo o
            JOIN clientes c ON c.id = o.cliente_id
            JOIN usuarios u ON u.id = o.usuario_id
            WHERE o.fecha_ingreso >= ? AND o.fecha_ingreso < ?
            ORDER BY o.fecha_ingreso
            """, inicio, fin);
    }

    private void domicilio(Workbook libro, Date desde, Date hasta) {
        hoja(libro, "Domicilio", List.of(
                new Col("Fecha", Tipo.FECHA, "fecha"), new Col("Hora", Tipo.ENTERO, "hora"),
                new Col("Servicio", Tipo.TEXTO, "tipo_servicio"), new Col("Estado", Tipo.ETIQUETA, "estado"),
                new Col("Cliente", Tipo.TEXTO, "cliente"), new Col("Cobrado", Tipo.DINERO, "monto_cobrado"),
                new Col("Método de pago", Tipo.ETIQUETA, "metodo_pago"), new Col("Técnico", Tipo.TEXTO, "tecnico")),
            """
            SELECT s.fecha, EXTRACT(HOUR FROM s.hora) AS hora, s.tipo_servicio, s.estado, c.nombre AS cliente,
                   s.monto_cobrado, s.metodo_pago, u.nombre AS tecnico
            FROM servicios_domicilio s
            JOIN clientes c ON c.id = s.cliente_id
            LEFT JOIN usuarios u ON u.id = s.realizado_por
            WHERE s.fecha BETWEEN ? AND ?
            ORDER BY s.fecha, s.hora
            """, desde, hasta);
    }

    /** La dimension: todo el catalogo como esta hoy (sin filtro de fechas). */
    private void productos(Workbook libro) {
        hoja(libro, "Productos", List.of(
                new Col("Código", Tipo.TEXTO, "codigo"), new Col("Producto", Tipo.TEXTO, "nombre"),
                new Col("Tipo", Tipo.ETIQUETA, "tipo_producto"), new Col("Marca", Tipo.TEXTO, "marca"),
                new Col("Perfil", Tipo.TEXTO, "llave_perfil"), new Col("Stock", Tipo.ENTERO, "cantidad_stock"),
                new Col("Stock mínimo", Tipo.ENTERO, "stock_minimo"), new Col("Stock bajo", Tipo.SI_NO, "bajo"),
                new Col("Precio compra", Tipo.DINERO, "precio_compra"), new Col("Precio venta", Tipo.DINERO, "precio_venta"),
                new Col("Precio duplicado", Tipo.DINERO, "precio_duplicado"),
                new Col("Valor del stock", Tipo.DINERO, "valor"), new Col("Activo", Tipo.SI_NO, "activo")),
            """
            SELECT codigo, nombre, tipo_producto, marca, llave_perfil, cantidad_stock, stock_minimo,
                   cantidad_stock <= stock_minimo AS bajo, precio_compra, precio_venta, precio_duplicado,
                   cantidad_stock * precio_compra AS valor, activo
            FROM productos
            ORDER BY tipo_producto, nombre
            """);
    }

    private void kardex(Workbook libro, Timestamp inicio, Timestamp fin) {
        hoja(libro, "Kardex", List.of(
                new Col("Fecha", Tipo.FECHA, "fecha"), new Col("Fecha y hora", Tipo.FECHA_HORA, "fecha_hora"),
                new Col("Código", Tipo.TEXTO, "codigo"), new Col("Producto", Tipo.TEXTO, "nombre"),
                new Col("Movimiento", Tipo.ETIQUETA, "tipo_movimiento"), new Col("Cantidad", Tipo.ENTERO, "cantidad"),
                new Col("Saldo anterior", Tipo.ENTERO, "saldo_anterior"), new Col("Saldo nuevo", Tipo.ENTERO, "saldo_nuevo"),
                new Col("Por", Tipo.ETIQUETA, "referencia_tipo"), new Col("Motivo", Tipo.TEXTO, "motivo"),
                new Col("Usuario", Tipo.TEXTO, "usuario")),
            """
            SELECT CAST(m.fecha_hora AS DATE) AS fecha, m.fecha_hora, p.codigo, p.nombre, m.tipo_movimiento,
                   m.cantidad, m.saldo_anterior, m.saldo_nuevo, m.referencia_tipo, m.motivo, u.nombre AS usuario
            FROM movimientos_inventario m
            JOIN productos p ON p.id = m.producto_id
            JOIN usuarios u ON u.id = m.usuario_id
            WHERE m.fecha_hora >= ? AND m.fecha_hora < ?
            ORDER BY m.fecha_hora, m.id
            """, inicio, fin);
    }

    private void compras(Workbook libro, Date desde, Date hasta) {
        hoja(libro, "Compras", List.of(
                new Col("Fecha", Tipo.FECHA, "fecha"), new Col("Distribuidor", Tipo.TEXTO, "proveedor"),
                new Col("Código", Tipo.TEXTO, "codigo"), new Col("Producto", Tipo.TEXTO, "nombre"),
                new Col("Cantidad", Tipo.ENTERO, "cantidad"), new Col("Precio unitario", Tipo.DINERO, "precio_unitario"),
                new Col("Total", Tipo.DINERO, "precio_total"), new Col("Pagado de caja", Tipo.SI_NO, "de_caja"),
                new Col("Usuario", Tipo.TEXTO, "usuario")),
            """
            SELECT co.fecha, pr.nombre AS proveedor, p.codigo, p.nombre, co.cantidad, co.precio_unitario,
                   co.precio_total, co.caja_diaria_id IS NOT NULL AS de_caja, u.nombre AS usuario
            FROM compras co
            JOIN proveedores pr ON pr.id = co.proveedor_id
            JOIN productos p ON p.id = co.producto_id
            JOIN usuarios u ON u.id = co.usuario_id
            WHERE co.fecha BETWEEN ? AND ?
            ORDER BY co.fecha, co.id
            """, desde, hasta);
    }

    private void cajas(Workbook libro, Timestamp inicio, Timestamp fin) {
        hoja(libro, "Cajas", List.of(
                new Col("Fecha", Tipo.FECHA, "fecha"), new Col("Apertura", Tipo.FECHA_HORA, "fecha_apertura"),
                new Col("Cierre", Tipo.FECHA_HORA, "fecha_cierre"), new Col("Usuario", Tipo.TEXTO, "usuario"),
                new Col("Estado", Tipo.ETIQUETA, "estado"), new Col("Monto inicial", Tipo.DINERO, "monto_apertura"),
                new Col("Ingresos", Tipo.DINERO, "monto_ingresos"), new Col("Egresos", Tipo.DINERO, "monto_egresos"),
                new Col("Yape", Tipo.DINERO, "monto_yape"), new Col("Plin", Tipo.DINERO, "monto_plin"),
                new Col("Tarjeta", Tipo.DINERO, "monto_tarjeta"), new Col("Efectivo contado", Tipo.DINERO, "monto_cierre"),
                new Col("Diferencia", Tipo.DINERO, "diferencia_cierre"), new Col("Observación", Tipo.TEXTO, "observacion_cierre")),
            """
            SELECT CAST(cd.fecha_apertura AS DATE) AS fecha, cd.fecha_apertura, cd.fecha_cierre, u.nombre AS usuario,
                   cd.estado, cd.monto_apertura, cd.monto_ingresos, cd.monto_egresos, cd.monto_yape, cd.monto_plin,
                   cd.monto_tarjeta, cd.monto_cierre, cd.diferencia_cierre, cd.observacion_cierre
            FROM cajas_diarias cd
            JOIN usuarios u ON u.id = cd.usuario_id
            WHERE cd.fecha_apertura >= ? AND cd.fecha_apertura < ?
            ORDER BY cd.fecha_apertura
            """, inicio, fin);
    }

    /** Ingresos y egresos puestos a mano en la caja (no son ventas ni cobros). */
    private void movimientosCaja(Workbook libro, Timestamp inicio, Timestamp fin) {
        hoja(libro, "MovimientosCaja", List.of(
                new Col("Fecha", Tipo.FECHA, "fecha"), new Col("Fecha y hora", Tipo.FECHA_HORA, "fecha_hora"),
                new Col("Tipo", Tipo.ETIQUETA, "tipo"), new Col("Monto", Tipo.DINERO, "monto"),
                new Col("Motivo", Tipo.TEXTO, "motivo"), new Col("Usuario", Tipo.TEXTO, "usuario")),
            """
            SELECT CAST(m.fecha_hora AS DATE) AS fecha, m.fecha_hora, m.tipo,
                   CASE WHEN m.tipo = 'EGRESO' THEN -m.monto ELSE m.monto END AS monto, m.motivo, u.nombre AS usuario
            FROM movimientos_caja m
            JOIN usuarios u ON u.id = m.usuario_id
            WHERE m.fecha_hora >= ? AND m.fecha_hora < ?
            ORDER BY m.fecha_hora
            """, inicio, fin);
    }

    private void leeme(Workbook libro, LocalDate desde, LocalDate hasta) {
        Worksheet hoja = libro.newWorksheet("Léeme");
        String[] lineas = {
            "Datos de la cerrajería para Power BI",
            "Fechas: del " + desde + " al " + hasta + " (Productos es el catálogo de hoy, sin filtro de fechas).",
            "Generado: " + LocalDateTime.now().withNano(0).toString().replace('T', ' '),
            "En Power BI: Obtener datos → Excel → marca las HOJAS (no las tablas): una hoja sin datos igual está, y así actualizar nunca falla.",
            "Relación: el Código de Ventas, Kardex y Compras con el Código de Productos.",
            "Ventas: Origen \"Anterior\" son las ventas de antes del sistema cargadas desde archivo (sin hora ni vendedor);",
            "  su Línea es \"Sin producto\" si lo escrito no coincidió con un producto del inventario (un servicio, por ejemplo).",
            "Ventas: Costo unitario es el precio de compra ACTUAL del producto, no el de la fecha de la venta.",
            "Cobros: montos negativos son devoluciones. Una venta anulada sigue apareciendo con Anulada = Sí: fíltrala.",
            "Para actualizar el tablero: descarga de nuevo, reemplaza este archivo con el mismo nombre y pulsa Actualizar.",
        };
        for (int i = 0; i < lineas.length; i++) {
            hoja.value(i, 0, lineas[i]);
        }
        hoja.style(0, 0).bold().set();
        hoja.width(0, 110);
    }

    /** Escribe cabecera y filas a medida que llegan, y las deja como tabla de Excel con el nombre de la hoja. */
    private void hoja(Workbook libro, String nombre, List<Col> cols, String sql, Object... parametros) {
        Worksheet hoja = libro.newWorksheet(nombre);
        for (int c = 0; c < cols.size(); c++) {
            hoja.value(0, c, cols.get(c).titulo());
            hoja.width(c, ancho(cols.get(c)));
        }
        hoja.range(0, 0, 0, cols.size() - 1).style().bold().set();
        hoja.freezePane(0, 1);

        int[] fila = {0};
        jdbc.query(sql, (ResultSet rs) -> {
            fila[0]++;
            for (int c = 0; c < cols.size(); c++) {
                escribir(hoja, fila[0], c, cols.get(c), rs);
            }
        }, parametros);

        if (fila[0] > 0) {
            hoja.range(0, 0, fila[0], cols.size() - 1)
                .createTable(cols.stream().map(Col::titulo).toArray(String[]::new))
                .setName(nombre).setDisplayName(nombre);
        }
    }

    static void escribir(Worksheet hoja, int r, int c, Col col, ResultSet rs) throws SQLException {
        Object valor = rs.getObject(col.columna());
        if (valor == null) {
            return;
        }
        switch (col.tipo()) {
            case TEXTO -> hoja.value(r, c, valor.toString());
            case ETIQUETA -> hoja.value(r, c, etiqueta(valor.toString()));
            case ENTERO -> hoja.value(r, c, ((Number) valor).longValue());
            case DINERO -> {
                hoja.value(r, c, (BigDecimal) valor);
                hoja.style(r, c).format("#,##0.00").set();
            }
            case FECHA -> {
                hoja.value(r, c, ((Date) valor).toLocalDate());
                hoja.style(r, c).format("dd/mm/yyyy").set();
            }
            case FECHA_HORA -> {
                hoja.value(r, c, ((Timestamp) valor).toLocalDateTime());
                hoja.style(r, c).format("dd/mm/yyyy hh:mm").set();
            }
            // La hora del dia (0-23) como numero: sirve para graficar las horas de mas venta
            case HORA -> hoja.value(r, c, ((Timestamp) valor).toLocalDateTime().getHour());
            case SI_NO -> hoja.value(r, c, Boolean.TRUE.equals(valor) ? "Sí" : "No");
        }
    }

    /** "EN_REPARACION" → "En reparación" (con las tildes de la tabla); lo demas, "ABIERTA" → "Abierta". */
    static String etiqueta(String codigo) {
        String conocida = ETIQUETAS.get(codigo);
        if (conocida != null) {
            return conocida;
        }
        String texto = codigo.replace('_', ' ').toLowerCase(Locale.ROOT);
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static double ancho(Col col) {
        return switch (col.tipo()) {
            case FECHA -> 12;
            case FECHA_HORA -> 17;
            case HORA, ENTERO, SI_NO -> 10;
            case DINERO -> 13;
            default -> Math.max(14, col.titulo().length() + 4);
        };
    }
}
