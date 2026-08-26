package com.panaderia.erp.produccion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ActualizarRecetaRequest(
        @NotEmpty @Valid List<RecetaItemRequest> items
) {
}
