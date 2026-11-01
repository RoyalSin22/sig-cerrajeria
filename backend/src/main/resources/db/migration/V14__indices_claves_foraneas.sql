-- Postgres no indexa solo las claves foraneas. Estas son las que se recorren
-- en cada venta, anulacion, cierre de caja y reporte: el detalle de ventas es
-- la tabla que mas crece (una fila por linea vendida).
CREATE INDEX IF NOT EXISTS idx_detalle_ventas_venta ON detalle_ventas (venta_id);
CREATE INDEX IF NOT EXISTS idx_detalle_ventas_producto ON detalle_ventas (producto_id);
CREATE INDEX IF NOT EXISTS idx_movcaja_caja ON movimientos_caja (caja_diaria_id);
CREATE INDEX IF NOT EXISTS idx_ventas_cliente ON ventas (cliente_id);
