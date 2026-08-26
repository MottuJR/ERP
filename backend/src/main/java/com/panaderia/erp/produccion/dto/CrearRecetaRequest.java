package com.panaderia.erp.produccion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CrearRecetaRequest(
        @NotNull Long productoId,
        @NotEmpty @Valid List<RecetaItemRequest> items
) {
}
