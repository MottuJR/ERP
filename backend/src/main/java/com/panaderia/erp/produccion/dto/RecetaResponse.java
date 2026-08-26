package com.panaderia.erp.produccion.dto;

import java.util.List;

public record RecetaResponse(
        Long id,
        Long productoId,
        String productoNombre,
        List<RecetaItemResponse> items
) {
}
