-- 1) Cobro de los servicios a domicilio. Se cobra al marcar el servicio como
-- REALIZADO, en la caja abierta de quien lo realizo (regla de negocio 1).
-- monto_cobrado = 0 es un servicio sin cobro (garantia, cortesia): no pasa
-- por caja y no tiene metodo de pago.
ALTER TABLE servicios_domicilio
  ADD COLUMN monto_cobrado  NUMERIC(12,2) CHECK (monto_cobrado >= 0),
  ADD COLUMN metodo_pago    VARCHAR(20) CHECK (metodo_pago IN ('EFECTIVO','YAPE','PLIN','TARJETA')),
  ADD COLUMN caja_diaria_id BIGINT REFERENCES cajas_diarias(id),
  ADD COLUMN realizado_por  BIGINT REFERENCES usuarios(id),
  ADD COLUMN fecha_realizado TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX idx_domicilio_fecha_realizado ON servicios_domicilio (fecha_realizado);

-- 2) Cancelar una orden de trabajo (el cliente se arrepiente o nunca la
-- recoge). Si se devuelve el adelanto, la devolucion queda como un pago de
-- tipo DEVOLUCION (egreso de la caja de quien la devuelve).
ALTER TABLE ordenes_trabajo DROP CONSTRAINT ordenes_trabajo_estado_check;
ALTER TABLE ordenes_trabajo ADD CONSTRAINT ordenes_trabajo_estado_check CHECK (estado IN (
  'RECIBIDO','EN_REPARACION','ESPERANDO_REPUESTO','LISTO','ENTREGADO','CANCELADO'
));
ALTER TABLE ordenes_trabajo ADD COLUMN motivo_cancelacion TEXT;

ALTER TABLE pagos_orden DROP CONSTRAINT pagos_orden_tipo_pago_check;
ALTER TABLE pagos_orden ADD CONSTRAINT pagos_orden_tipo_pago_check
  CHECK (tipo_pago IN ('ADELANTO','LIQUIDACION','DEVOLUCION'));

CREATE INDEX idx_pagos_orden_fecha ON pagos_orden (fecha_hora);
