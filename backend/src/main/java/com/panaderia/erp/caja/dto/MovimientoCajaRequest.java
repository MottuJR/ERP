package com.panaderia.erp.caja.dto;

import com.panaderia.erp.caja.TipoMovimientoCaja;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MovimientoCajaRequest(
        @NotNull TipoMovimientoCaja tipo,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal monto,
        @NotBlank String concepto
) {
}
