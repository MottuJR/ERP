package com.panaderia.erp.produccion.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecetaResponse(
        Long id,
        Long productoId,
        String productoNombre,
        BigDecimal rendimiento,
        List<RecetaItemResponse> items
) {
}
