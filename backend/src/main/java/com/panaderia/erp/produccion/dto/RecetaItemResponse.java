package com.panaderia.erp.produccion.dto;

import java.math.BigDecimal;

public record RecetaItemResponse(
        Long insumoId,
        String insumoNombre,
        String unidadMedida,
        BigDecimal cantidad
) {
}
