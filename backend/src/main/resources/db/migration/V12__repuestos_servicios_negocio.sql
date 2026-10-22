-- Repuestos de licuadora (cuchillas, vasos, empaques Oster): la cerrajeria
-- tambien repara licuadoras y vende sus repuestos.
ALTER TABLE productos DROP CONSTRAINT productos_tipo_producto_check;
ALTER TABLE productos ADD CONSTRAINT productos_tipo_producto_check CHECK (tipo_producto IN (
  'LLAVE_VIRGEN','LLAVE_COLOR','LLAVE_DISENO','CHAPA','CILINDRO',
  'CANDADO','LLAVERO','ARGOLLA','ACCESORIO','REPUESTO_LICUADORA'
));

-- Servicio rapido en el mostrador (abrir un candado, afilar una cuchilla):
-- una linea de venta sin producto, con descripcion y precio. No mueve stock.
ALTER TABLE detalle_ventas ALTER COLUMN producto_id DROP NOT NULL;
ALTER TABLE detalle_ventas ADD COLUMN descripcion VARCHAR(150);
ALTER TABLE detalle_ventas ADD CONSTRAINT detalle_ventas_producto_o_servicio
  CHECK (producto_id IS NOT NULL OR descripcion IS NOT NULL);

-- Datos del negocio (una sola fila): nombre, telefono y direccion que salen en
-- la app y en los mensajes de WhatsApp a los clientes. Se editan en Ajustes.
CREATE TABLE negocio (
  id INT PRIMARY KEY CHECK (id = 1),
  nombre VARCHAR(100) NOT NULL,
  telefono VARCHAR(20) NOT NULL,
  direccion VARCHAR(200) NOT NULL,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO negocio (id, nombre, telefono, direccion) VALUES
  (1, 'Cerrajería Barrera', '982616709', 'Av. Alfonso Ugarte N° 115, frente al Mercado N° 2, Surco');
