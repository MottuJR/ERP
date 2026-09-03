package com.panaderia.erp.produccion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CrearRecetaRequest(
        @NotNull Long productoId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal rendimiento,
        @NotEmpty @Valid List<RecetaItemRequest> items
) {
}
