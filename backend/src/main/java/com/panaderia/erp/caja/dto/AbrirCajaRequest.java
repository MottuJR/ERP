package com.panaderia.erp.caja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AbrirCajaRequest(
        @NotNull @DecimalMin(value = "0.0") BigDecimal montoInicial
) {
}
