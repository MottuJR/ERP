-- Un producto NO vendido por peso también puede tener un código PLU: la balanza puede
-- configurarse para imprimirle una cantidad de unidades en vez de un peso (ver EscaneoService),
-- por ejemplo para contar facturas más rápido pesándolas en tanda. Antes esta restricción a
-- nivel base de datos lo prohibía junto con la validación de ProductoService.
ALTER TABLE productos DROP CONSTRAINT chk_producto_peso_variable;

ALTER TABLE productos ADD CONSTRAINT chk_producto_peso_variable CHECK (
    se_vende_por_peso = false
    OR (codigo_plu IS NOT NULL AND codigo_barras IS NULL)
);
