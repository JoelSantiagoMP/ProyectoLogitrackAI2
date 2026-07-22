
INSERT INTO usuario (username, password, rol) VALUES 
('admin_prueba', '123456', 'ADMIN'),
('empleado_juan', '123456', 'EMPLEADO')
ON CONFLICT (username) DO NOTHING;


INSERT INTO bodega (nombre, ubicacion, capacidad, encargado) VALUES 
('Bodega Central', 'Bogotá - Zona Industrial', 5000, 'Carlos Gómez'),
('Bodega Norte', 'Medellín - Autopista', 2000, 'Ana Martínez');

INSERT INTO producto (nombre, categoria, stock, precio) VALUES 
('Laptop Dell XPS 15', 'Electrónica', 50, 1500.00),
('Monitor LG 27 pulgadas', 'Electrónica', 120, 300.00),
('Silla Ergonómica', 'Mobiliario', 15, 850.00);