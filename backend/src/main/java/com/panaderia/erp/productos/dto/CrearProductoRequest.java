package com.panaderia.erp.productos.dto;

import com.panaderia.erp.productos.TipoProducto;
import com.panaderia.erp.productos.UnidadMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CrearProductoRequest(
        @NotBlank String nombre,
        @NotNull Long categoriaId,
        @NotNull TipoProducto tipo,
        @NotNull Boolean seVendePorPeso,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal precioVenta,
        @NotNull UnidadMedida unidadMedida,
        String codigoBarras,
        String codigoPLU,
        @DecimalMin(value = "0.0") BigDecimal stockMinimo
) {
}
