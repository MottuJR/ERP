package com.panaderia.erp.auth.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ActualizarComisionRequest(
        @NotNull @DecimalMin(value = "0.0") @DecimalMax(value = "100.0") BigDecimal porcentajeComision
) {
}
