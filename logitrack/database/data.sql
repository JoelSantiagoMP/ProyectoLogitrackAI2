SET search_path TO logitrack;

-- 1. Insertar usuarios (Contraseña para ambos en este ejemplo: 123456 cifrada con BCrypt)
INSERT INTO usuario (username, password, rol) VALUES
('admin_logitrack', '$2a$10$vkbNymWQeG0w7nzNIKO0beRmKtXv.VjEZHIo.n46zY3ctaVqpDFtq', 'ADMIN'),
('empleado_1', '$2a$10$9z4/YJb0KWZTsbBaluYbiOhI.5PB4/zB0/ypXQwo/Sp7WiGo4EB.m', 'EMPLEADO');

-- 2. Insertar bodegas (Asociadas al usuario encargado ID 1, que es el admin)
INSERT INTO bodega (nombre, ubicacion, capacidad, encargado_id) VALUES
('Bodega Principal Bucaramanga', 'Zona Industrial Norte', 5000, 1),
('Bodega Secundaria Sur', 'Autopista Sur Km 3', 2500, 1);

-- 3. Insertar productos de prueba
INSERT INTO producto (nombre, categoria, precio) VALUES
('Laptop Gamer XYZ', 'Tecnología', 3500000.00),
('Silla Ergonómica de Oficina', 'Muebles', 450000.00),
('Mouse Inalámbrico Ergonomico', 'Tecnología', 85000.00);

-- 4. Insertar inventario inicial por bodega
INSERT INTO inventario_bodega (bodega_id, producto_id, cantidad) VALUES
(1, 1, 15), -- 15 Laptops en Bodega Principal
(1, 2, 30), -- 30 Sillas en Bodega Principal
(2, 3, 50); -- 50 Mouses en Bodega Secundaria

-- 5. Insertar un movimiento de prueba (ENTRADA realizada por el usuario 1)
INSERT INTO movimiento (tipo_movimiento, usuario_id, bodega_destino_id) VALUES
('ENTRADA', 1, 1);

-- 6. Insertar el detalle del movimiento anterior
INSERT INTO detalle_movimiento (movimiento_id, producto_id, cantidad) VALUES
(1, 1, 15);

-- 7. Insertar un registro de auditoría de prueba
INSERT INTO auditoria (tipo_operacion, usuario_responsable_id, entidad_afectada, valor_anterior, valor_nuevo) VALUES
('INSERT', 1, 'PRODUCTO', NULL, '{"id": 1, "nombre": "Laptop Gamer XYZ", "precio": 3500000.00}');
