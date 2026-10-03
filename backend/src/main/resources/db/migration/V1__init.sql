-- ============================================================
-- SIG-CERRAJERÍA — SCRIPT DDL (PostgreSQL 16)
-- Migración Flyway V1 — validado en Fase 6 (Diseño de la Base de Datos)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- USUARIOS
CREATE TABLE usuarios (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL CHECK (role IN ('ROLE_ADMIN','ROLE_EMPLOYEE')),
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- CLIENTES
CREATE TABLE clientes (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(150) NOT NULL,
  telefono VARCHAR(20) NOT NULL,
  direccion VARCHAR(255),
  observaciones TEXT,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clientes_nombre_trgm ON clientes USING GIN (nombre gin_trgm_ops);
CREATE INDEX idx_clientes_telefono_trgm ON clientes USING GIN (telefono gin_trgm_ops);

-- CAJAS DIARIAS
CREATE TABLE cajas_diarias (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  fecha_apertura TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_cierre TIMESTAMP WITHOUT TIME ZONE,
  monto_apertura NUMERIC(12,2) NOT NULL CHECK (monto_apertura >= 0),
  monto_cierre NUMERIC(12,2) CHECK (monto_cierre >= 0),
  monto_ingresos NUMERIC(12,2) NOT NULL DEFAULT 0.00,
  monto_egresos NUMERIC(12,2) NOT NULL DEFAULT 0.00,
  estado VARCHAR(15) NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA','CERRADA'))
);

-- Garantiza una única caja ABIERTA por usuario (regla de negocio 1)
CREATE UNIQUE INDEX idx_caja_unica_abierta_por_usuario
  ON cajas_diarias (usuario_id) WHERE estado = 'ABIERTA';

-- MOVIMIENTOS DE CAJA
CREATE TABLE movimientos_caja (
  id BIGSERIAL PRIMARY KEY,
  caja_diaria_id BIGINT NOT NULL REFERENCES cajas_diarias(id),
  tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('INGRESO','EGRESO')),
  monto NUMERIC(12,2) NOT NULL CHECK (monto > 0),
  motivo VARCHAR(255) NOT NULL,
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  fecha_hora TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- PRODUCTOS
CREATE TABLE productos (
  id BIGSERIAL PRIMARY KEY,
  codigo VARCHAR(50) UNIQUE NOT NULL,
  nombre VARCHAR(150) NOT NULL,
  tipo_producto VARCHAR(30) NOT NULL CHECK (tipo_producto IN (
    'LLAVE_VIRGEN','LLAVE_COLOR','LLAVE_DISENO','CHAPA','CILINDRO',
    'CANDADO','LLAVERO','ARGOLLA','ACCESORIO'
  )),
  marca VARCHAR(100),
  cantidad_stock INT NOT NULL DEFAULT 0 CHECK (cantidad_stock >= 0),
  stock_minimo INT NOT NULL DEFAULT 5 CHECK (stock_minimo >= 0),
  precio_compra NUMERIC(12,2) NOT NULL CHECK (precio_compra >= 0),
  precio_venta NUMERIC(12,2) NOT NULL CHECK (precio_venta >= 0),
  imagen_url VARCHAR(500),
  llave_perfil VARCHAR(50),
  llave_color VARCHAR(30),
  llave_diseno_tematica VARCHAR(50),
  producto_modelo VARCHAR(100),
  producto_tamano VARCHAR(30),
  producto_material VARCHAR(50),
  atributos JSONB,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_productos_nombre_trgm ON productos USING GIN (nombre gin_trgm_ops);
CREATE INDEX idx_productos_codigo_trgm ON productos USING GIN (codigo gin_trgm_ops);
CREATE INDEX idx_productos_tipo ON productos (tipo_producto);
CREATE INDEX idx_productos_atributos ON productos USING GIN (atributos);

-- MOVIMIENTOS DE INVENTARIO (KÁRDEX)
CREATE TABLE movimientos_inventario (
  id BIGSERIAL PRIMARY KEY,
  producto_id BIGINT NOT NULL REFERENCES productos(id),
  tipo_movimiento VARCHAR(10) NOT NULL CHECK (tipo_movimiento IN ('ENTRADA','SALIDA','AJUSTE')),
  cantidad INT NOT NULL CHECK (cantidad <> 0),
  saldo_anterior INT NOT NULL,
  saldo_nuevo INT NOT NULL,
  referencia_tipo VARCHAR(20) CHECK (referencia_tipo IN ('VENTA','COMPRA','AJUSTE','ORDEN_TRABAJO')),
  referencia_id BIGINT,
  motivo VARCHAR(255),
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  fecha_hora TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movinv_producto ON movimientos_inventario (producto_id, fecha_hora);

-- VENTAS
CREATE TABLE ventas (
  id BIGSERIAL PRIMARY KEY,
  caja_diaria_id BIGINT NOT NULL REFERENCES cajas_diarias(id),
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  cliente_id BIGINT REFERENCES clientes(id),
  fecha_hora TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  monto_total NUMERIC(12,2) NOT NULL CHECK (monto_total >= 0),
  metodo_pago VARCHAR(20) NOT NULL CHECK (metodo_pago IN ('EFECTIVO','YAPE','PLIN','TARJETA','MIXTO'))
);

CREATE INDEX idx_ventas_fecha ON ventas (fecha_hora);
CREATE INDEX idx_ventas_caja ON ventas (caja_diaria_id);

-- DETALLE DE VENTAS
CREATE TABLE detalle_ventas (
  id BIGSERIAL PRIMARY KEY,
  venta_id BIGINT NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
  producto_id BIGINT NOT NULL REFERENCES productos(id),
  cantidad INT NOT NULL CHECK (cantidad > 0),
  precio_unitario NUMERIC(12,2) NOT NULL CHECK (precio_unitario >= 0),
  subtotal NUMERIC(12,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED
);

-- ÓRDENES DE TRABAJO
CREATE TABLE ordenes_trabajo (
  id BIGSERIAL PRIMARY KEY,
  numero_orden VARCHAR(20) UNIQUE NOT NULL,
  cliente_id BIGINT NOT NULL REFERENCES clientes(id),
  producto_recibido VARCHAR(150) NOT NULL,
  problema TEXT NOT NULL,
  diagnostico TEXT,
  precio_total NUMERIC(12,2) NOT NULL CHECK (precio_total >= 0),
  monto_pagado NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (monto_pagado >= 0),
  saldo_pendiente NUMERIC(12,2) GENERATED ALWAYS AS (precio_total - monto_pagado) STORED,
  estado VARCHAR(20) NOT NULL DEFAULT 'RECIBIDO' CHECK (estado IN (
    'RECIBIDO','EN_REPARACION','ESPERANDO_REPUESTO','LISTO','ENTREGADO'
  )),
  fecha_ingreso TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_prometida TIMESTAMP WITHOUT TIME ZONE,
  fecha_entrega TIMESTAMP WITHOUT TIME ZONE,
  observaciones TEXT,
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ordenes_estado ON ordenes_trabajo (estado);
CREATE INDEX idx_ordenes_cliente ON ordenes_trabajo (cliente_id);

-- PAGOS DE ÓRDENES (adelantos y liquidaciones)
CREATE TABLE pagos_orden (
  id BIGSERIAL PRIMARY KEY,
  orden_trabajo_id BIGINT NOT NULL REFERENCES ordenes_trabajo(id),
  caja_diaria_id BIGINT NOT NULL REFERENCES cajas_diarias(id),
  monto NUMERIC(12,2) NOT NULL CHECK (monto > 0),
  tipo_pago VARCHAR(15) NOT NULL CHECK (tipo_pago IN ('ADELANTO','LIQUIDACION')),
  metodo_pago VARCHAR(20) NOT NULL CHECK (metodo_pago IN ('EFECTIVO','YAPE','PLIN','TARJETA','MIXTO')),
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  fecha_hora TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pagos_orden ON pagos_orden (orden_trabajo_id);

-- SERVICIOS A DOMICILIO
CREATE TABLE servicios_domicilio (
  id BIGSERIAL PRIMARY KEY,
  cliente_id BIGINT NOT NULL REFERENCES clientes(id),
  direccion VARCHAR(255) NOT NULL,
  telefono VARCHAR(20) NOT NULL,
  fecha DATE NOT NULL,
  hora TIME NOT NULL,
  tipo_servicio VARCHAR(100) NOT NULL,
  observaciones TEXT,
  estado VARCHAR(15) NOT NULL DEFAULT 'PROGRAMADO' CHECK (estado IN (
    'PROGRAMADO','EN_CAMINO','REALIZADO','CANCELADO'
  )),
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_domicilio_fecha ON servicios_domicilio (fecha, hora);

-- PROVEEDORES
CREATE TABLE proveedores (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(150) NOT NULL,
  telefono VARCHAR(20),
  direccion VARCHAR(255)
);

-- COMPRAS
CREATE TABLE compras (
  id BIGSERIAL PRIMARY KEY,
  proveedor_id BIGINT NOT NULL REFERENCES proveedores(id),
  producto_id BIGINT NOT NULL REFERENCES productos(id),
  cantidad INT NOT NULL CHECK (cantidad > 0),
  precio_unitario NUMERIC(12,2) NOT NULL CHECK (precio_unitario >= 0),
  precio_total NUMERIC(12,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,
  fecha DATE NOT NULL DEFAULT CURRENT_DATE,
  caja_diaria_id BIGINT REFERENCES cajas_diarias(id),
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id)
);

CREATE INDEX idx_compras_producto ON compras (producto_id);

-- AUDIT LOG
CREATE TABLE audit_log (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
  accion VARCHAR(30) NOT NULL CHECK (accion IN (
    'CREAR','EDITAR','ELIMINAR','CAMBIO_ESTADO','AJUSTE_INVENTARIO',
    'ANULACION','APERTURA_CAJA','CIERRE_CAJA'
  )),
  entidad VARCHAR(50) NOT NULL,
  entidad_id BIGINT NOT NULL,
  detalle JSONB,
  fecha_hora TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_entidad ON audit_log (entidad, entidad_id);
CREATE INDEX idx_audit_fecha ON audit_log (fecha_hora);
