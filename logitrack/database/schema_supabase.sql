-- =============================================================================
-- LogiTrack IQ — esquema unificado PostgreSQL / Supabase
-- Esquema: logitrack
-- Ejecutar en el SQL Editor de Supabase (o psql) como un solo script.
-- Contraseña de usuarios de prueba: 123456 (BCrypt).
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS logitrack;
SET search_path TO logitrack;

-- -----------------------------------------------------------------------------
-- Tablas base
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS usuario (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    CONSTRAINT rol_check CHECK (rol IN ('ADMIN', 'EMPLEADO', 'AGENTE'))
);

CREATE TABLE IF NOT EXISTS bodega (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(150),
    capacidad INTEGER NOT NULL,
    encargado VARCHAR(150) NOT NULL,
    CONSTRAINT capacidad_check CHECK (capacidad > 0)
);

CREATE TABLE IF NOT EXISTS proveedor (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    contacto VARCHAR(150),
    dias_entrega INTEGER NOT NULL,
    CONSTRAINT proveedor_dias_entrega_check CHECK (dias_entrega BETWEEN 1 AND 90)
);

CREATE TABLE IF NOT EXISTS producto (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL UNIQUE,
    categoria VARCHAR(100),
    precio DECIMAL(10, 2) NOT NULL,
    proveedor_principal_id BIGINT,
    CONSTRAINT precio_check CHECK (precio >= 0),
    CONSTRAINT fk_producto_proveedor FOREIGN KEY (proveedor_principal_id) REFERENCES proveedor(id)
);

CREATE TABLE IF NOT EXISTS inventario_bodega (
    id BIGSERIAL PRIMARY KEY,
    bodega_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT inventario_cantidad_check CHECK (cantidad >= 0),
    CONSTRAINT fk_inventario_bodega FOREIGN KEY (bodega_id) REFERENCES bodega(id),
    CONSTRAINT fk_inventario_producto FOREIGN KEY (producto_id) REFERENCES producto(id),
    CONSTRAINT uq_bodega_producto UNIQUE (bodega_id, producto_id)
);

CREATE TABLE IF NOT EXISTS movimiento (
    id BIGSERIAL PRIMARY KEY,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tipo_movimiento VARCHAR(20) NOT NULL,
    usuario_id BIGINT NOT NULL,
    bodega_origen_id BIGINT,
    bodega_destino_id BIGINT,
    CONSTRAINT tipo_movimiento_check CHECK (tipo_movimiento IN ('ENTRADA', 'SALIDA', 'TRANSFERENCIA')),
    CONSTRAINT fk_movimiento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_movimiento_origen FOREIGN KEY (bodega_origen_id) REFERENCES bodega(id),
    CONSTRAINT fk_movimiento_destino FOREIGN KEY (bodega_destino_id) REFERENCES bodega(id)
);

CREATE TABLE IF NOT EXISTS detalle_movimiento (
    id BIGSERIAL PRIMARY KEY,
    movimiento_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INTEGER NOT NULL,
    CONSTRAINT cantidad_detalle_check CHECK (cantidad > 0),
    CONSTRAINT fk_detalle_movimiento FOREIGN KEY (movimiento_id) REFERENCES movimiento(id),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
);

CREATE TABLE IF NOT EXISTS auditoria (
    id BIGSERIAL PRIMARY KEY,
    tipo_operacion VARCHAR(20) NOT NULL,
    fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_responsable_id BIGINT NOT NULL,
    entidad_afectada VARCHAR(50) NOT NULL,
    entidad_id BIGINT,
    valor_anterior TEXT,
    valor_nuevo TEXT,
    CONSTRAINT tipo_operacion_check CHECK (tipo_operacion IN ('INSERT', 'UPDATE', 'DELETE')),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_responsable_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS orden_compra (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    proveedor_id BIGINT NOT NULL,
    bodega_destino_id BIGINT NOT NULL,
    cantidad INTEGER NOT NULL,
    precio_unitario DOUBLE PRECISION NOT NULL,
    total DOUBLE PRECISION NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    creado_por_id BIGINT NOT NULL,
    pdf BYTEA,
    fecha_generacion_pdf TIMESTAMP,
    CONSTRAINT orden_cantidad_check CHECK (cantidad > 0),
    CONSTRAINT orden_estado_check CHECK (estado IN ('BORRADOR', 'APROBADA', 'RECIBIDA', 'CANCELADA')),
    CONSTRAINT fk_orden_producto FOREIGN KEY (producto_id) REFERENCES producto(id),
    CONSTRAINT fk_orden_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id),
    CONSTRAINT fk_orden_bodega FOREIGN KEY (bodega_destino_id) REFERENCES bodega(id),
    CONSTRAINT fk_orden_autor FOREIGN KEY (creado_por_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS resumen_panel (
    id BIGSERIAL PRIMARY KEY,
    fecha DATE NOT NULL,
    contenido_json TEXT NOT NULL,
    autor_id BIGINT NOT NULL,
    CONSTRAINT uk_resumen_panel_fecha UNIQUE (fecha),
    CONSTRAINT fk_resumen_autor FOREIGN KEY (autor_id) REFERENCES usuario(id)
);

-- -----------------------------------------------------------------------------
-- Compatibilidad con bases ya creadas (reto anterior / IQ)
-- -----------------------------------------------------------------------------

ALTER TABLE logitrack.usuario DROP CONSTRAINT IF EXISTS rol_check;
ALTER TABLE logitrack.usuario
    ADD CONSTRAINT rol_check CHECK (rol IN ('ADMIN', 'EMPLEADO', 'AGENTE'));

ALTER TABLE logitrack.auditoria ADD COLUMN IF NOT EXISTS entidad_id BIGINT;

ALTER TABLE logitrack.producto ADD COLUMN IF NOT EXISTS proveedor_principal_id BIGINT;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_producto_proveedor'
    ) THEN
        ALTER TABLE logitrack.producto
            ADD CONSTRAINT fk_producto_proveedor
            FOREIGN KEY (proveedor_principal_id) REFERENCES logitrack.proveedor(id);
    END IF;
END $$;

ALTER TABLE logitrack.orden_compra ADD COLUMN IF NOT EXISTS pdf BYTEA;
ALTER TABLE logitrack.orden_compra ADD COLUMN IF NOT EXISTS fecha_generacion_pdf TIMESTAMP;

-- Migrar encargado_id (FK usuario) → encargado (texto libre)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'logitrack' AND table_name = 'bodega' AND column_name = 'encargado_id'
    ) THEN
        ALTER TABLE logitrack.bodega ADD COLUMN IF NOT EXISTS encargado VARCHAR(150);
        UPDATE logitrack.bodega b
        SET encargado = u.username
        FROM logitrack.usuario u
        WHERE b.encargado_id = u.id
          AND (b.encargado IS NULL OR TRIM(b.encargado) = '');
        UPDATE logitrack.bodega
        SET encargado = 'Sin asignar'
        WHERE encargado IS NULL OR TRIM(encargado) = '';
        ALTER TABLE logitrack.bodega DROP CONSTRAINT IF EXISTS fk_bodega_encargado;
        ALTER TABLE logitrack.bodega DROP COLUMN encargado_id;
        ALTER TABLE logitrack.bodega ALTER COLUMN encargado SET NOT NULL;
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- Datos de demostración IQ
-- Hash BCrypt de 123456 (mismo valor que data.sql histórico)
-- -----------------------------------------------------------------------------

INSERT INTO usuario (username, password, rol) VALUES
('admin_logitrack', '$2a$10$vkbNymWQeG0w7nzNIKO0beRmKtXv.VjEZHIo.n46zY3ctaVqpDFtq', 'ADMIN'),
('empleado_1', '$2a$10$9z4/YJb0KWZTsbBaluYbiOhI.5PB4/zB0/ypXQwo/Sp7WiGo4EB.m', 'EMPLEADO'),
('agente_mcp', '$2a$10$vkbNymWQeG0w7nzNIKO0beRmKtXv.VjEZHIo.n46zY3ctaVqpDFtq', 'AGENTE')
ON CONFLICT (username) DO UPDATE SET
    rol = EXCLUDED.rol,
    password = EXCLUDED.password;

INSERT INTO proveedor (nombre, contacto, dias_entrega)
SELECT 'Proveedor Andina SAS', 'compras@andina.test', 10
WHERE NOT EXISTS (SELECT 1 FROM proveedor WHERE nombre = 'Proveedor Andina SAS');

INSERT INTO proveedor (nombre, contacto, dias_entrega)
SELECT 'Suministros Caribe Ltda', 'ventas@caribe.test', 7
WHERE NOT EXISTS (SELECT 1 FROM proveedor WHERE nombre = 'Suministros Caribe Ltda');

INSERT INTO bodega (nombre, ubicacion, capacidad, encargado)
SELECT 'Bodega Principal Bucaramanga', 'Zona Industrial Norte', 5000, 'Carlos Ramírez'
WHERE NOT EXISTS (SELECT 1 FROM bodega WHERE nombre = 'Bodega Principal Bucaramanga');

INSERT INTO bodega (nombre, ubicacion, capacidad, encargado)
SELECT 'Bodega Secundaria Sur', 'Autopista Sur Km 3', 80, 'Sandra López'
WHERE NOT EXISTS (SELECT 1 FROM bodega WHERE nombre = 'Bodega Secundaria Sur');

INSERT INTO producto (nombre, categoria, precio, proveedor_principal_id)
SELECT 'Resma Papel A4', 'Oficina', 18500,
       (SELECT id FROM proveedor WHERE nombre = 'Proveedor Andina SAS')
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Resma Papel A4');

INSERT INTO producto (nombre, categoria, precio, proveedor_principal_id)
SELECT 'Toner Laser Negro', 'Oficina', 120000,
       (SELECT id FROM proveedor WHERE nombre = 'Suministros Caribe Ltda')
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Toner Laser Negro');

INSERT INTO producto (nombre, categoria, precio, proveedor_principal_id)
SELECT 'Laptop Gamer XYZ', 'Tecnología', 3500000,
       (SELECT id FROM proveedor WHERE nombre = 'Proveedor Andina SAS')
WHERE NOT EXISTS (SELECT 1 FROM producto WHERE nombre = 'Laptop Gamer XYZ');

-- Stock bajo en Papel y Toner (stock < punto de reorden si hay consumo).
-- Bodega Sur pequeña + mucho stock → ocupación crítica (≥ 90 %).
INSERT INTO inventario_bodega (bodega_id, producto_id, cantidad)
SELECT b.id, p.id, v.cantidad
FROM (VALUES
    ('Bodega Principal Bucaramanga', 'Resma Papel A4', 8),
    ('Bodega Principal Bucaramanga', 'Toner Laser Negro', 3),
    ('Bodega Principal Bucaramanga', 'Laptop Gamer XYZ', 15),
    ('Bodega Secundaria Sur', 'Laptop Gamer XYZ', 75)
) AS v(bodega, producto, cantidad)
JOIN bodega b ON b.nombre = v.bodega
JOIN producto p ON p.nombre = v.producto
ON CONFLICT (bodega_id, producto_id) DO UPDATE SET cantidad = EXCLUDED.cantidad;

-- ENTRADA inicial + SALIDAS recientes (consumo diario > 0 → productos en riesgo)
INSERT INTO movimiento (fecha, tipo_movimiento, usuario_id, bodega_destino_id)
SELECT NOW() - INTERVAL '20 days', 'ENTRADA', u.id, b.id
FROM usuario u, bodega b
WHERE u.username = 'admin_logitrack'
  AND b.nombre = 'Bodega Principal Bucaramanga'
  AND NOT EXISTS (
      SELECT 1 FROM movimiento m
      WHERE m.tipo_movimiento = 'ENTRADA' AND m.usuario_id = u.id
        AND m.bodega_destino_id = b.id AND m.fecha < NOW() - INTERVAL '15 days'
  );

INSERT INTO detalle_movimiento (movimiento_id, producto_id, cantidad)
SELECT m.id, p.id, 100
FROM movimiento m
JOIN usuario u ON u.id = m.usuario_id
JOIN producto p ON p.nombre = 'Resma Papel A4'
WHERE u.username = 'admin_logitrack' AND m.tipo_movimiento = 'ENTRADA'
  AND NOT EXISTS (
      SELECT 1 FROM detalle_movimiento d WHERE d.movimiento_id = m.id AND d.producto_id = p.id
  );

INSERT INTO movimiento (fecha, tipo_movimiento, usuario_id, bodega_origen_id)
SELECT NOW() - INTERVAL '5 days', 'SALIDA', u.id, b.id
FROM usuario u, bodega b
WHERE u.username = 'admin_logitrack'
  AND b.nombre = 'Bodega Principal Bucaramanga'
  AND NOT EXISTS (
      SELECT 1 FROM movimiento m
      WHERE m.tipo_movimiento = 'SALIDA' AND m.usuario_id = u.id
        AND m.bodega_origen_id = b.id
  );

INSERT INTO detalle_movimiento (movimiento_id, producto_id, cantidad)
SELECT m.id, p.id, v.cant
FROM movimiento m
JOIN usuario u ON u.id = m.usuario_id
JOIN (VALUES ('Resma Papel A4', 90), ('Toner Laser Negro', 60)) AS v(nombre, cant) ON TRUE
JOIN producto p ON p.nombre = v.nombre
WHERE u.username = 'admin_logitrack' AND m.tipo_movimiento = 'SALIDA'
  AND NOT EXISTS (
      SELECT 1 FROM detalle_movimiento d WHERE d.movimiento_id = m.id AND d.producto_id = p.id
  );

INSERT INTO orden_compra (
    producto_id, proveedor_id, bodega_destino_id, cantidad, precio_unitario, total,
    fecha_creacion, estado, creado_por_id
)
SELECT p.id, pr.id, b.id, 40, p.precio, 40 * p.precio,
       NOW(), 'BORRADOR', a.id
FROM producto p
JOIN proveedor pr ON pr.id = p.proveedor_principal_id
JOIN bodega b ON b.nombre = 'Bodega Principal Bucaramanga'
JOIN usuario a ON a.username = 'agente_mcp'
WHERE p.nombre = 'Resma Papel A4'
  AND NOT EXISTS (
      SELECT 1 FROM orden_compra o
      WHERE o.producto_id = p.id AND o.estado = 'BORRADOR' AND o.creado_por_id = a.id
  );

INSERT INTO orden_compra (
    producto_id, proveedor_id, bodega_destino_id, cantidad, precio_unitario, total,
    fecha_creacion, estado, creado_por_id
)
SELECT p.id, pr.id, b.id, 20, p.precio, 20 * p.precio,
       NOW(), 'BORRADOR', a.id
FROM producto p
JOIN proveedor pr ON pr.id = p.proveedor_principal_id
JOIN bodega b ON b.nombre = 'Bodega Principal Bucaramanga'
JOIN usuario a ON a.username = 'agente_mcp'
WHERE p.nombre = 'Toner Laser Negro'
  AND NOT EXISTS (
      SELECT 1 FROM orden_compra o
      WHERE o.producto_id = p.id AND o.estado = 'BORRADOR' AND o.creado_por_id = a.id
  );

INSERT INTO auditoria (tipo_operacion, usuario_responsable_id, entidad_afectada, entidad_id, valor_anterior, valor_nuevo)
SELECT 'INSERT', u.id, 'OrdenCompra', o.id, NULL, 'BORRADOR semilla IQ'
FROM usuario u
JOIN orden_compra o ON o.creado_por_id = u.id
WHERE u.username = 'agente_mcp'
  AND NOT EXISTS (
      SELECT 1 FROM auditoria a
      WHERE a.entidad_afectada = 'OrdenCompra' AND a.entidad_id = o.id
  );

-- Credenciales:
--   admin_logitrack / 123456  → ADMIN
--   agente_mcp      / 123456  → AGENTE (MCP y n8n)
--   empleado_1      / 123456  → EMPLEADO
