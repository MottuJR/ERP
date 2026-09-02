package com.panaderia.erp.clientes.dto;

import com.panaderia.erp.ventas.MedioPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagoClienteRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal monto,
        @NotNull MedioPago medioPago
) {
}
