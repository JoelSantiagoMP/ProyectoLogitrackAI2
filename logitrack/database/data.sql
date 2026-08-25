-- LogiTrack IQ — datos de demostración
-- Contraseña de todos los usuarios: 123456 (BCrypt)
-- Ejecutar después de schema.sql en una base vacía.

SET search_path TO logitrack;

-- Usuarios
INSERT INTO usuario (username, password, rol) VALUES
('admin_logitrack', '$2a$10$vkbNymWQeG0w7nzNIKO0beRmKtXv.VjEZHIo.n46zY3ctaVqpDFtq', 'ADMIN'),
('empleado_1', '$2a$10$9z4/YJb0KWZTsbBaluYbiOhI.5PB4/zB0/ypXQwo/Sp7WiGo4EB.m', 'EMPLEADO'),
('agente_mcp', '$2a$10$vkbNymWQeG0w7nzNIKO0beRmKtXv.VjEZHIo.n46zY3ctaVqpDFtq', 'AGENTE');

-- Proveedores
INSERT INTO proveedor (nombre, contacto, dias_entrega) VALUES
('Proveedor Andina SAS', 'compras@andina.test', 10),
('Suministros Caribe Ltda', 'ventas@caribe.test', 7);

-- Bodegas (Sur con capacidad 80 → ocupación crítica ≥ 90 % con 75 laptops)
INSERT INTO bodega (nombre, ubicacion, capacidad, encargado) VALUES
('Bodega Principal Bucaramanga', 'Zona Industrial Norte', 5000, 'Carlos Ramírez'),
('Bodega Secundaria Sur', 'Autopista Sur Km 3', 80, 'Sandra López');

-- Productos con proveedor principal
INSERT INTO producto (nombre, categoria, precio, proveedor_principal_id) VALUES
('Resma Papel A4', 'Oficina', 18500, 1),
('Toner Laser Negro', 'Oficina', 120000, 2),
('Laptop Gamer XYZ', 'Tecnología', 3500000, 1);

-- Inventario inicial (stock bajo en Papel y Toner → productos en riesgo)
INSERT INTO inventario_bodega (bodega_id, producto_id, cantidad) VALUES
(1, 1, 8),
(1, 2, 3),
(1, 3, 15),
(2, 3, 75);

-- ENTRADA histórica (inventario inicial representado en movimientos)
INSERT INTO movimiento (fecha, tipo_movimiento, usuario_id, bodega_destino_id) VALUES
(NOW() - INTERVAL '20 days', 'ENTRADA', 1, 1);

INSERT INTO detalle_movimiento (movimiento_id, producto_id, cantidad) VALUES
(1, 1, 100);

-- SALIDAS recientes (consumo diario > 0 → punto de reorden superado)
INSERT INTO movimiento (fecha, tipo_movimiento, usuario_id, bodega_origen_id) VALUES
(NOW() - INTERVAL '5 days', 'SALIDA', 1, 1);

INSERT INTO detalle_movimiento (movimiento_id, producto_id, cantidad) VALUES
(2, 1, 90),
(2, 2, 60);

-- Órdenes BORRADOR semilla (creadas por agente_mcp)
INSERT INTO orden_compra (
    producto_id, proveedor_id, bodega_destino_id, cantidad, precio_unitario, total,
    fecha_creacion, estado, creado_por_id
) VALUES
(1, 1, 1, 40, 18500, 740000, NOW(), 'BORRADOR', 3),
(2, 2, 1, 20, 120000, 2400000, NOW(), 'BORRADOR', 3);

-- Auditoría de ejemplo
INSERT INTO auditoria (tipo_operacion, usuario_responsable_id, entidad_afectada, entidad_id, valor_anterior, valor_nuevo) VALUES
('INSERT', 3, 'OrdenCompra', 1, NULL, 'BORRADOR semilla IQ'),
('INSERT', 3, 'OrdenCompra', 2, NULL, 'BORRADOR semilla IQ');

-- Credenciales:
--   admin_logitrack / 123456  → ADMIN
--   agente_mcp      / 123456  → AGENTE (MCP y n8n)
--   empleado_1      / 123456  → EMPLEADO
