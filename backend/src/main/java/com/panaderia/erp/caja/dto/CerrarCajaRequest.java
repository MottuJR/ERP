package com.panaderia.erp.caja.dto;

import com.panaderia.erp.caja.MedioPagoComision;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CerrarCajaRequest(
        @NotNull @DecimalMin(value = "0.0") BigDecimal montoFinal,
        MedioPagoComision comisionMedioPago
) {
}
