-- Cuánto rinde la tanda de la receta (en la unidad de venta del producto). Las recetas ya
-- cargadas quedan en 1 (sin cambios: sus ítems ya estaban escritos por unidad de producto).
ALTER TABLE recetas ADD COLUMN rendimiento NUMERIC(12, 3) NOT NULL DEFAULT 1;
