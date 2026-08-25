-- Migra bodega.encargado_id (FK) → bodega.encargado (texto libre)
SET search_path TO logitrack;

ALTER TABLE bodega ADD COLUMN IF NOT EXISTS encargado VARCHAR(150);

UPDATE bodega b
SET encargado = u.username
FROM usuario u
WHERE b.encargado_id = u.id
  AND (b.encargado IS NULL OR TRIM(b.encargado) = '');

UPDATE bodega
SET encargado = 'Sin asignar'
WHERE encargado IS NULL OR TRIM(encargado) = '';

ALTER TABLE bodega DROP CONSTRAINT IF EXISTS fk_bodega_encargado;
ALTER TABLE bodega DROP COLUMN IF EXISTS encargado_id;
ALTER TABLE bodega ALTER COLUMN encargado SET NOT NULL;
