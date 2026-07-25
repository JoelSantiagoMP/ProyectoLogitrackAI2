CREATE SCHEMA IF NOT EXISTS logitrack;
SET search_path TO logitrack;

CREATE TABLE IF NOT EXISTS usuario (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    CONSTRAINT rol_check CHECK (rol IN ('ADMIN', 'EMPLEADO'))
);

CREATE TABLE IF NOT EXISTS bodega (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(150),
    capacidad INTEGER NOT NULL,
    encargado_id BIGINT NOT NULL,
    CONSTRAINT capacidad_check CHECK (capacidad >= 0),
    CONSTRAINT fk_bodega_encargado FOREIGN KEY (encargado_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS producto (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    categoria VARCHAR(100),
    precio DECIMAL(10, 2) NOT NULL,
    CONSTRAINT precio_check CHECK (precio >= 0)
);

-- Nueva tabla intermedia para gestionar el stock real por bodega
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
    valor_anterior TEXT,
    valor_nuevo TEXT,
    CONSTRAINT tipo_operacion_check CHECK (tipo_operacion IN ('INSERT', 'UPDATE', 'DELETE')),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_responsable_id) REFERENCES usuario(id)
);