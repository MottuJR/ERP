package com.panaderia.erp.inventario.dto;

import com.panaderia.erp.inventario.ItemTipo;
import com.panaderia.erp.inventario.TipoMovimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MovimientoManualRequest(
        @NotNull ItemTipo itemTipo,
        @NotNull Long itemId,
        @NotNull TipoMovimiento tipo,
        @NotNull BigDecimal cantidad,
        @NotBlank String motivo
) {
}
