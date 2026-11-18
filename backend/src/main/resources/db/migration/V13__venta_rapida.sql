-- Botones de "Venta rápida" en Vender: lo que el Administrador elige tener a un toque
-- (patrón "Favoritos" de los POS de mostrador). Por defecto ninguno: la pantalla
-- muestra entonces lo más vendido del último mes.
ALTER TABLE productos ADD COLUMN venta_rapida BOOLEAN NOT NULL DEFAULT FALSE;
