-- Migración: módulo Proveedores y relación con Productos (Supabase / PostgreSQL)
-- Idempotente: seguro de ejecutar más de una vez.

SET search_path TO logitrack;

-- Tabla proveedores (renombrar desde proveedor si aplica)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'logitrack' AND table_name = 'proveedor'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'logitrack' AND table_name = 'proveedores'
    ) THEN
        ALTER TABLE proveedor RENAME TO proveedores;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS proveedores (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    dias_entrega INTEGER NOT NULL,
    CONSTRAINT proveedores_dias_entrega_check CHECK (dias_entrega BETWEEN 1 AND 90)
);

-- Migrar columna contacto → email si existe
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'logitrack' AND table_name = 'proveedores' AND column_name = 'contacto'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'logitrack' AND table_name = 'proveedores' AND column_name = 'email'
    ) THEN
        ALTER TABLE proveedores RENAME COLUMN contacto TO email;
    END IF;
END $$;

-- Datos de prueba
INSERT INTO proveedores (nombre, email, dias_entrega)
SELECT 'Proveedor Andina SAS', 'compras@andina.test', 10
WHERE NOT EXISTS (SELECT 1 FROM proveedores WHERE nombre = 'Proveedor Andina SAS');

INSERT INTO proveedores (nombre, email, dias_entrega)
SELECT 'Suministros Caribe Ltda', 'ventas@caribe.test', 7
WHERE NOT EXISTS (SELECT 1 FROM proveedores WHERE nombre = 'Suministros Caribe Ltda');

-- Columna proveedor_id en producto (renombrar desde proveedor_principal_id si aplica)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'logitrack' AND table_name = 'producto' AND column_name = 'proveedor_principal_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'logitrack' AND table_name = 'producto' AND column_name = 'proveedor_id'
    ) THEN
        ALTER TABLE producto RENAME COLUMN proveedor_principal_id TO proveedor_id;
    END IF;
END $$;

ALTER TABLE producto
    ADD COLUMN IF NOT EXISTS proveedor_id BIGINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_producto_proveedor'
    ) THEN
        ALTER TABLE producto DROP CONSTRAINT fk_producto_proveedor;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_producto_proveedor'
    ) THEN
        ALTER TABLE producto
            ADD CONSTRAINT fk_producto_proveedor
            FOREIGN KEY (proveedor_id) REFERENCES proveedores(id);
    END IF;
END $$;

COMMENT ON COLUMN producto.proveedor_id IS
    'Proveedor principal del producto; requerido para alertas de riesgo y órdenes automáticas.';
