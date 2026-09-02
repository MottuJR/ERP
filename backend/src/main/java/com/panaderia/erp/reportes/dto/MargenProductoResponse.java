package com.panaderia.erp.reportes.dto;

import java.math.BigDecimal;

public record MargenProductoResponse(
        Long productoId,
        String productoNombre,
        BigDecimal precioVenta,
        BigDecimal costoInsumos,
        BigDecimal margen,
        BigDecimal margenPorcentual
) {
}
