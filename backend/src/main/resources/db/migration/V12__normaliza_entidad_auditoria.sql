-- Los ajustes de stock registraban la entidad con el nombre crudo del enum ItemTipo
-- ("PRODUCTO"/"INSUMO"), mientras que las altas/bajas de Producto ya usaban "Producto".
-- Eso hacía que el filtro de Auditoría por entidad mostrara la misma entidad duplicada
-- con distinta capitalización. Se normaliza a un único casing por entidad.
UPDATE registro_auditoria SET entidad = 'Producto' WHERE entidad = 'PRODUCTO';
UPDATE registro_auditoria SET entidad = 'Insumo' WHERE entidad = 'INSUMO';
