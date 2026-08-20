package com.panaderia.erp.inventario.dto;

import com.panaderia.erp.inventario.Insumo;
import com.panaderia.erp.productos.UnidadMedida;

import java.math.BigDecimal;

public record InsumoResponse(
        Long id,
        String nombre,
        UnidadMedida unidadMedida,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        BigDecimal costoUnitario
) {

    public static InsumoResponse from(Insumo insumo) {
        return new InsumoResponse(
                insumo.getId(),
                insumo.getNombre(),
                insumo.getUnidadMedida(),
                insumo.getStockActual(),
                insumo.getStockMinimo(),
                insumo.getCostoUnitario());
    }
}
