-- Cambio de contrasena (propia o restablecida por el administrador).
-- password_cambiado_en invalida las sesiones abiertas: /auth/refresh rechaza
-- los refresh tokens emitidos antes de esa marca. Es TIMESTAMPTZ porque se
-- compara contra el "iat" del JWT, que es un instante UTC.
ALTER TABLE usuarios ADD COLUMN password_cambiado_en TIMESTAMPTZ;

ALTER TABLE audit_log DROP CONSTRAINT audit_log_accion_check;
ALTER TABLE audit_log ADD CONSTRAINT audit_log_accion_check CHECK (accion IN (
  'CREAR','EDITAR','ELIMINAR','CAMBIO_ESTADO','AJUSTE_INVENTARIO',
  'ANULACION','APERTURA_CAJA','CIERRE_CAJA','CAMBIO_PASSWORD'
));
