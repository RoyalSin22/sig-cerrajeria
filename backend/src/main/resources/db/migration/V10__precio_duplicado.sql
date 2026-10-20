-- Precio del servicio de duplicado (material + corte), aparte del precio de la
-- llave sola, que se vende rara vez (a otro cerrajero, de adorno). NULL: el
-- producto no se ofrece en "Duplicado de llave".
ALTER TABLE productos
  ADD COLUMN precio_duplicado NUMERIC(12,2) CHECK (precio_duplicado >= 0);

-- Hasta ahora el duplicado cobraba el precio de venta de la llave virgen:
-- se conserva ese comportamiento hasta que se ponga el precio de la llave sola.
UPDATE productos SET precio_duplicado = precio_venta WHERE tipo_producto = 'LLAVE_VIRGEN';

-- La linea de la venta recuerda si fue un duplicado o la llave sola.
ALTER TABLE detalle_ventas
  ADD COLUMN es_duplicado BOOLEAN NOT NULL DEFAULT FALSE;
