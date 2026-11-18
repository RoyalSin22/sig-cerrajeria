# Tablero en Power BI

El sistema entrega un Excel con todos los datos del negocio para armar tableros en Power BI Desktop. No hace falta conectarse a la base de datos: se descarga el archivo y se carga.

## 1. Descargar los datos

En **Reportes** (solo el Administrador), elige las fechas arriba y toca **Descargar Excel**. El archivo se llama siempre `cerrajeria-datos.xlsx`.

Para el historial de antes del sistema, en la misma pantalla está **Ventas de antes del sistema**: sube un CSV con `fecha`, `producto`, `cantidad` y `precio` (o `total`); `codigo` y `metodo_pago` son opcionales. Esas ventas quedan aparte: no tocan la caja ni el stock.

## 2. Cargar en Power BI

1. **Obtener datos → Libro de Excel** → elige `cerrajeria-datos.xlsx`.
2. Marca las **hojas** (ícono de hoja, no el de tabla): una hoja sin datos igual existe, así actualizar nunca falla.
3. **Cargar**. Power BI reconoce solo las fechas y los montos. Si alguna columna `Fecha` aparece con hora (`0:00`), en **Transformar datos** cámbiale el tipo a **Fecha**: la relación con el calendario lo necesita.

Para actualizar: descarga de nuevo, guarda encima del archivo anterior (mismo nombre y carpeta) y pulsa **Actualizar** en Power BI.

## 3. Qué trae cada hoja

| Hoja | Una fila por… | Para qué sirve |
|---|---|---|
| **Ventas** | línea vendida (y cada venta anterior cargada) | Qué se vende, cuándo, duplicado vs. llave sola vs. servicio |
| **Cobros** | pago recibido (una venta mixta, en varias filas) | Dinero por método de pago: efectivo, Yape, Plin, tarjeta |
| **Taller** | orden de trabajo | Trabajos, saldo pendiente, días en el taller, garantías |
| **Domicilio** | servicio a domicilio | Servicios por tipo y por técnico |
| **Productos** | producto (catálogo de hoy) | Stock, stock bajo, valor del inventario |
| **Kardex** | movimiento de stock | Entradas, salidas y ajustes |
| **Compras** | compra a un distribuidor | Gasto en mercadería |
| **Cajas** | caja abierta | Cierres y diferencias |
| **MovimientosCaja** | ingreso o egreso puesto a mano | Gastos y entradas que no son ventas |

Cosas a tener en cuenta:
- **Ventas → Origen**: `Sistema` o `Anterior` (cargada desde archivo, sin hora ni vendedor).
- **Ventas → Anulada** y **Cobros → Anulada**: las ventas anuladas siguen en el archivo; fíltralas (`Anulada = "No"`).
- **Ventas → Costo unitario** es el precio de compra **actual** del producto, no el de la fecha de la venta. Los servicios no tienen costo.
- **Cobros → Monto** negativo: devolución de un adelanto del taller.
- **Ventas → Hora** es la hora del día (0 a 23), para ver las horas de más venta.

## 4. Modelo (relaciones)

Crea una tabla de fechas (**Modelado → Nueva tabla**):

```DAX
Calendario =
ADDCOLUMNS (
    CALENDAR ( DATE ( 2024, 1, 1 ), TODAY () ),
    "Año", YEAR ( [Date] ),
    "Mes", FORMAT ( [Date], "mmm yyyy" ),
    "MesNumero", YEAR ( [Date] ) * 100 + MONTH ( [Date] ),
    "Día de la semana", FORMAT ( [Date], "dddd" ),
    "DíaNumero", WEEKDAY ( [Date], 2 )
)
```

Ordena `Mes` por `MesNumero` y `Día de la semana` por `DíaNumero` (**Ordenar por columna**).

Relaciones (una a varias, de la izquierda a la derecha):

| De | A |
|---|---|
| `Calendario[Date]` | `Ventas[Fecha]`, `Cobros[Fecha]`, `Kardex[Fecha]`, `Compras[Fecha]`, `Cajas[Fecha]`, `Domicilio[Fecha]`, `MovimientosCaja[Fecha]` |
| `Calendario[Date]` | `Taller[Fecha ingreso]` |
| `Productos[Código]` | `Ventas[Código]`, `Kardex[Código]`, `Compras[Código]` |

## 5. Medidas listas para copiar

```DAX
Ventas S/ = CALCULATE ( SUM ( Ventas[Subtotal] ), Ventas[Anulada] = "No" )

Unidades = CALCULATE ( SUM ( Ventas[Cantidad] ), Ventas[Anulada] = "No" )

Costo S/ =
CALCULATE (
    SUMX ( Ventas, Ventas[Cantidad] * Ventas[Costo unitario] ),
    Ventas[Anulada] = "No"
)

Ganancia aprox. S/ = [Ventas S/] - [Costo S/]

Margen % = DIVIDE ( [Ganancia aprox. S/], [Ventas S/] )

Ticket promedio =
DIVIDE (
    CALCULATE ( SUM ( Ventas[Subtotal] ), Ventas[Origen] = "Sistema", Ventas[Anulada] = "No" ),
    CALCULATE ( DISTINCTCOUNT ( Ventas[Venta] ), Ventas[Origen] = "Sistema", Ventas[Anulada] = "No" )
)

Duplicados = CALCULATE ( [Unidades], Ventas[Línea] = "Duplicado" )

Cobrado S/ = CALCULATE ( SUM ( Cobros[Monto] ), Cobros[Anulada] = "No" )

Saldo por cobrar taller = SUM ( Taller[Saldo] )

Valor del inventario = SUM ( Productos[Valor del stock] )

Productos con stock bajo = CALCULATE ( COUNTROWS ( Productos ), Productos[Stock bajo] = "Sí", Productos[Activo] = "Sí" )

Ventas mes anterior = CALCULATE ( [Ventas S/], DATEADD ( Calendario[Date], -1, MONTH ) )

Crecimiento vs. mes anterior = DIVIDE ( [Ventas S/] - [Ventas mes anterior], [Ventas mes anterior] )
```

## 6. Tablero sugerido (una página)

- **Tarjetas**: Ventas S/, Ganancia aprox. S/, Ticket promedio, Saldo por cobrar taller, Productos con stock bajo.
- **Columnas**: Ventas S/ por `Calendario[Mes]` (con Crecimiento vs. mes anterior en la información sobre herramientas).
- **Barras**: los 10 productos con más Unidades (`Ventas[Producto]`, filtro Top N).
- **Anillo**: Cobrado S/ por `Cobros[Método de pago]`.
- **Matriz**: `Calendario[Día de la semana]` × `Ventas[Hora]` con Ventas S/ (formato condicional): qué días y horas se vende más.
- **Segmentadores**: `Calendario[Mes]`, `Ventas[Tipo]`, `Ventas[Línea]`.
