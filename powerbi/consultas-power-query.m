// Consultas de Power Query para cerrajeria-datos.xlsx (generadas desde ExportacionPowerBiService).
// Cada una fija el tipo de cada columna en vez de adivinarlo: funciona igual con hojas vacias
// (Taller sin ordenes) o columnas vacias (Cierre de una caja abierta), y cuando despues lleguen datos.
// Uso: Transformar datos -> elegir la consulta -> Inicio -> Editor avanzado -> borrar todo y pegar su bloque.
// Requiere el parametro RutaExcel (ver POWER_BI.md, paso 1).

// ===================== Ventas =====================
let
    Origen = Excel.Workbook(File.Contents(RutaExcel), null, false),
    Hoja = Origen{[Item = "Ventas", Kind = "Sheet"]}[Data],
    Encabezados = Table.PromoteHeaders(Hoja, [PromoteAllScalars = true]),
    Tipos = Table.TransformColumnTypes(
        Encabezados,
        {
            {"Fecha", type date},
            {"Hora", Int64.Type},
            {"Fecha y hora", type datetime},
            {"Origen", type text},
            {"Venta", Int64.Type},
            {"Código", type text},
            {"Producto", type text},
            {"Tipo", type text},
            {"Marca", type text},
            {"Línea", type text},
            {"Cantidad", Int64.Type},
            {"Precio unitario", Currency.Type},
            {"Subtotal", Currency.Type},
            {"Costo unitario", Currency.Type},
            {"Método de pago", type text},
            {"Vendedor", type text},
            {"Cliente", type text},
            {"Anulada", type text}
        },
        "es-PE"
    )
in
    Tipos

// ===================== Cobros =====================
let
    Origen = Excel.Workbook(File.Contents(RutaExcel), null, false),
    Hoja = Origen{[Item = "Cobros", Kind = "Sheet"]}[Data],
    Encabezados = Table.PromoteHeaders(Hoja, [PromoteAllScalars = true]),
    Tipos = Table.TransformColumnTypes(
        Encabezados,
        {
            {"Fecha", type date},
            {"Hora", Int64.Type},
            {"Fecha y hora", type datetime},
            {"Origen", type text},
            {"Referencia", type text},
            {"Concepto", type text},
            {"Método de pago", type text},
            {"Monto", Currency.Type},
            {"Usuario", type text},
            {"Anulada", type text}
        },
        "es-PE"
    )
in
    Tipos

// ===================== Taller =====================
let
    Origen = Excel.Workbook(File.Contents(RutaExcel), null, false),
    Hoja = Origen{[Item = "Taller", Kind = "Sheet"]}[Data],
    Encabezados = Table.PromoteHeaders(Hoja, [PromoteAllScalars = true]),
    Tipos = Table.TransformColumnTypes(
        Encabezados,
        {
            {"Orden", type text},
            {"Fecha ingreso", type date},
            {"Fecha entrega", type date},
            {"Estado", type text},
            {"Cliente", type text},
            {"Trabajo", type text},
            {"Precio total", Currency.Type},
            {"Pagado", Currency.Type},
            {"Saldo", Currency.Type},
            {"Garantía", type text},
            {"Días en el taller", Int64.Type},
            {"Registró", type text}
        },
        "es-PE"
    )
in
    Tipos

// ===================== Domicilio =====================
let
    Origen = Excel.Workbook(File.Contents(RutaExcel), null, false),
    Hoja = Origen{[Item = "Domicilio", Kind = "Sheet"]}[Data],
    Encabezados = Table.PromoteHeaders(Hoja, [PromoteAllScalars = true]),
    Tipos = Table.TransformColumnTypes(
        Encabezados,
        {
            {"Fecha", type date},
            {"Hora", Int64.Type},
            {"Servicio", type text},
            {"Estado", type text},
            {"Cliente", type text},
            {"Cobrado", Currency.Type},
            {"Método de pago", type text},
            {"Técnico", type text}
        },
        "es-PE"
    )
in
    Tipos

// ===================== Productos =====================
let
    Origen = Excel.Workbook(File.Contents(RutaExcel), null, false),
    Hoja = Origen{[Item = "Productos", Kind = "Sheet"]}[Data],
    Encabezados = Table.PromoteHeaders(Hoja, [PromoteAllScalars = true]),
    Tipos = Table.TransformColumnTypes(
        Encabezados,
        {
            {"Código", type text},
            {"Producto", type text},
            {"Tipo", type text},
            {"Marca", type text},
            {"Perfil", type text},
            {"Stock", Int64.Type},
            {"Stock mínimo", Int64.Type},
            {"Stock bajo", type text},
            {"Precio compra", Currency.Type},
            {"Precio venta", Currency.Type},
            {"Precio duplicado", Currency.Type},
            {"Valor del stock", Currency.Type},
            {"Activo", type text}
        },
        "es-PE"
    )
in
    Tipos

// ===================== Kardex =====================
let
    Origen = Excel.Workbook(File.Contents(RutaExcel), null, false),
    Hoja = Origen{[Item = "Kardex", Kind = "Sheet"]}[Data],
    Encabezados = Table.PromoteHeaders(Hoja, [PromoteAllScalars = true]),
    Tipos = Table.TransformColumnTypes(
        Encabezados,
        {
            {"Fecha", type date},
            {"Fecha y hora", type datetime},
            {"Código", type text},
            {"Producto", type text},
            {"Movimiento", type text},
            {"Cantidad", Int64.Type},
            {"Saldo anterior", Int64.Type},
            {"Saldo nuevo", Int64.Type},
            {"Por", type text},
            {"Motivo", type text},
            {"Usuario", type text}
        },
        "es-PE"
    )
in
    Tipos

// ===================== Compras =====================
let
    Origen = Excel.Workbook(File.Contents(RutaExcel), null, false),
    Hoja = Origen{[Item = "Compras", Kind = "Sheet"]}[Data],
    Encabezados = Table.PromoteHeaders(Hoja, [PromoteAllScalars = true]),
    Tipos = Table.TransformColumnTypes(
        Encabezados,
        {
            {"Fecha", type date},
            {"Distribuidor", type text},
            {"Código", type text},
            {"Producto", type text},
            {"Cantidad", Int64.Type},
            {"Precio unitario", Currency.Type},
            {"Total", Currency.Type},
            {"Pagado de caja", type text},
            {"Usuario", type text}
        },
        "es-PE"
    )
in
    Tipos

// ===================== Cajas =====================
let
    Origen = Excel.Workbook(File.Contents(RutaExcel), null, false),
    Hoja = Origen{[Item = "Cajas", Kind = "Sheet"]}[Data],
    Encabezados = Table.PromoteHeaders(Hoja, [PromoteAllScalars = true]),
    Tipos = Table.TransformColumnTypes(
        Encabezados,
        {
            {"Fecha", type date},
            {"Apertura", type datetime},
            {"Cierre", type datetime},
            {"Usuario", type text},
            {"Estado", type text},
            {"Monto inicial", Currency.Type},
            {"Ingresos", Currency.Type},
            {"Egresos", Currency.Type},
            {"Yape", Currency.Type},
            {"Plin", Currency.Type},
            {"Tarjeta", Currency.Type},
            {"Efectivo contado", Currency.Type},
            {"Diferencia", Currency.Type},
            {"Observación", type text}
        },
        "es-PE"
    )
in
    Tipos

// ===================== MovimientosCaja =====================
let
    Origen = Excel.Workbook(File.Contents(RutaExcel), null, false),
    Hoja = Origen{[Item = "MovimientosCaja", Kind = "Sheet"]}[Data],
    Encabezados = Table.PromoteHeaders(Hoja, [PromoteAllScalars = true]),
    Tipos = Table.TransformColumnTypes(
        Encabezados,
        {
            {"Fecha", type date},
            {"Fecha y hora", type datetime},
            {"Tipo", type text},
            {"Monto", Currency.Type},
            {"Motivo", type text},
            {"Usuario", type text}
        },
        "es-PE"
    )
in
    Tipos
