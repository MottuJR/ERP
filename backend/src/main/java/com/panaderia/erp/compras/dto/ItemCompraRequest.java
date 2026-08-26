package com.panaderia.erp.compras.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ItemCompraRequest(
        @NotNull Long insumoId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal cantidad,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal costoUnitario
) {
}
