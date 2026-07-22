
CREATE TABLE IF NOT EXISTS usuario (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL
);


CREATE TABLE IF NOT EXISTS bodega (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(150),
    capacidad INT NOT NULL,
    encargado VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS producto (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    categoria VARCHAR(100),
    stock INT NOT NULL DEFAULT 0,
    precio DECIMAL(10, 2) NOT NULL
);


CREATE TABLE IF NOT EXISTS movimiento (
    id SERIAL PRIMARY KEY,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tipo VARCHAR(20) NOT NULL,
    usuario_id INT NOT NULL REFERENCES usuario(id),
    bodega_origen_id INT REFERENCES bodega(id),
    bodega_destino_id INT REFERENCES bodega(id)
);


CREATE TABLE IF NOT EXISTS movimiento_detalle (
    id SERIAL PRIMARY KEY,
    movimiento_id INT NOT NULL REFERENCES movimiento(id),
    producto_id INT NOT NULL REFERENCES producto(id),
    cantidad INT NOT NULL
);


CREATE TABLE IF NOT EXISTS auditoria (
    id SERIAL PRIMARY KEY,
    tipo_operacion VARCHAR(20) NOT NULL, -- INSERT, UPDATE, DELETE
    fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_responsable VARCHAR(50) NOT NULL,
    entidad_afectada VARCHAR(50) NOT NULL,
    valores_anteriores TEXT, -- Guardaremos el estado previo en formato JSON
    valores_nuevos TEXT      -- Guardaremos el estado nuevo en formato JSON
);