-- Anulacion de ventas: revierte stock (Kardex) y el ingreso de caja si la
-- caja que proceso la venta sigue abierta. Se preserva el registro original
-- (no se borra fila) para mantener la trazabilidad (regla de negocio 2).
ALTER TABLE ventas
  ADD COLUMN anulada BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN fecha_anulacion TIMESTAMP WITHOUT TIME ZONE;
