-- Auditoria con detalle: audit_log.detalle (JSONB, ya existia sin usarse)
-- guarda descripcion, motivo y cambios antes/despues. Acciones nuevas.
ALTER TABLE audit_log DROP CONSTRAINT audit_log_accion_check;
ALTER TABLE audit_log ADD CONSTRAINT audit_log_accion_check CHECK (accion IN (
  'CREAR','EDITAR','ELIMINAR','CAMBIO_ESTADO','AJUSTE_INVENTARIO',
  'ANULACION','APERTURA_CAJA','CIERRE_CAJA','CAMBIO_PASSWORD',
  'MOVIMIENTO_CAJA','GARANTIA'
));

-- Cierre de caja: la diferencia (contado - esperado) queda guardada, y si no
-- cuadra, la explicacion de quien cerro.
ALTER TABLE cajas_diarias
  ADD COLUMN diferencia_cierre NUMERIC(12,2),
  ADD COLUMN observacion_cierre TEXT;

-- Garantia: una orden sin cobro que vuelve sobre una orden ya entregada.
ALTER TABLE ordenes_trabajo
  ADD COLUMN es_garantia BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN orden_origen_id BIGINT REFERENCES ordenes_trabajo(id);
CREATE INDEX idx_ordenes_origen ON ordenes_trabajo (orden_origen_id);
