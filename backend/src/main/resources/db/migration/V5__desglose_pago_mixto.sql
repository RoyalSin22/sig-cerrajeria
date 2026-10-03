-- Desglose de pago cuando metodo_pago = 'MIXTO' en una venta (ej. parte en
-- efectivo, parte en Yape). Para ventas con un solo metodo, esta tabla queda
-- vacia: el propio ventas.metodo_pago ya es suficiente.
CREATE TABLE pagos_venta (
    id BIGSERIAL PRIMARY KEY,
    venta_id BIGINT NOT NULL REFERENCES ventas(id),
    metodo_pago VARCHAR(20) NOT NULL CHECK (metodo_pago IN ('EFECTIVO','YAPE','PLIN','TARJETA')),
    monto NUMERIC(12,2) NOT NULL CHECK (monto > 0)
);

CREATE INDEX idx_pagos_venta_venta_id ON pagos_venta(venta_id);
