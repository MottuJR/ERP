package com.panaderia.erp.inventario.dto;

import com.panaderia.erp.productos.UnidadMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InsumoRequest(
        @NotBlank String nombre,
        @NotNull UnidadMedida unidadMedida,
        @DecimalMin(value = "0.0") BigDecimal stockMinimo,
        @DecimalMin(value = "0.0") BigDecimal costoUnitario
) {
}
