package com.panaderia.erp.reportes.dto;

import java.math.BigDecimal;

public record StockCriticoItemResponse(
        Long id,
        String nombre,
        BigDecimal stockActual,
        BigDecimal stockMinimo
) {
}
