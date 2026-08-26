package com.panaderia.erp.compras.dto;

import java.math.BigDecimal;

public record DetalleCompraResponse(
        Long id,
        Long insumoId,
        String insumoNombre,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal subtotal
) {
}
