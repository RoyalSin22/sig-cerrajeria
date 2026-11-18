-- Ventas de antes del sistema (cuaderno, Excel), solo para analisis y Power BI.
-- A proposito NO son filas de "ventas": no tienen caja (regla 1) ni movieron
-- el stock (regla 2), porque ese dinero y esa mercaderia ya se contaron al
-- empezar a usar el sistema. Se cargan por lote desde un archivo y se pueden
-- borrar para volver a subirlas.
CREATE TABLE ventas_anteriores (
  id BIGSERIAL PRIMARY KEY,
  fecha DATE NOT NULL,
  -- Lo escrito en el archivo; si coincide con un producto del inventario, se enlaza
  descripcion VARCHAR(150) NOT NULL,
  producto_id BIGINT REFERENCES productos(id),
  cantidad INT NOT NULL CHECK (cantidad > 0),
  precio_unitario NUMERIC(12,2) NOT NULL CHECK (precio_unitario >= 0),
  metodo_pago VARCHAR(20) CHECK (metodo_pago IN ('EFECTIVO','YAPE','PLIN','TARJETA')),
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ventas_anteriores_fecha ON ventas_anteriores (fecha);
