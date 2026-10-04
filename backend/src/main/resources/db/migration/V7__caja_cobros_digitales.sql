-- Desglose de los cobros que NO entran al cajon (Yape, Plin, tarjeta).
-- monto_ingresos/monto_egresos siguen siendo los totales de la caja; el
-- efectivo que deberia haber al cerrar es:
--   monto_apertura + monto_ingresos - monto_egresos - (yape + plin + tarjeta)
-- Cada columna es el neto del metodo: cobros menos anulaciones.
ALTER TABLE cajas_diarias
  ADD COLUMN monto_yape    NUMERIC(12,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN monto_plin    NUMERIC(12,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN monto_tarjeta NUMERIC(12,2) NOT NULL DEFAULT 0.00;

-- Relleno de cajas existentes desde los registros de origen. Las ventas
-- anuladas se excluyen: su ingreso y su egreso ya se compensan en los
-- totales (solo se pueden anular con la caja aun abierta). Los pagos de
-- orden MIXTO no tienen desglose y quedan contados como efectivo.
WITH cobros AS (
    SELECT v.caja_diaria_id, v.metodo_pago, v.monto_total AS monto
    FROM ventas v
    WHERE NOT v.anulada AND v.metodo_pago IN ('YAPE', 'PLIN', 'TARJETA')
  UNION ALL
    SELECT v.caja_diaria_id, pv.metodo_pago, pv.monto
    FROM pagos_venta pv
    JOIN ventas v ON v.id = pv.venta_id
    WHERE NOT v.anulada AND v.metodo_pago = 'MIXTO'
      AND pv.metodo_pago IN ('YAPE', 'PLIN', 'TARJETA')
  UNION ALL
    SELECT po.caja_diaria_id, po.metodo_pago, po.monto
    FROM pagos_orden po
    WHERE po.metodo_pago IN ('YAPE', 'PLIN', 'TARJETA')
),
totales AS (
  SELECT caja_diaria_id,
         SUM(monto) FILTER (WHERE metodo_pago = 'YAPE')    AS yape,
         SUM(monto) FILTER (WHERE metodo_pago = 'PLIN')    AS plin,
         SUM(monto) FILTER (WHERE metodo_pago = 'TARJETA') AS tarjeta
  FROM cobros
  GROUP BY caja_diaria_id
)
UPDATE cajas_diarias c
SET monto_yape    = COALESCE(t.yape, 0),
    monto_plin    = COALESCE(t.plin, 0),
    monto_tarjeta = COALESCE(t.tarjeta, 0)
FROM totales t
WHERE t.caja_diaria_id = c.id;
