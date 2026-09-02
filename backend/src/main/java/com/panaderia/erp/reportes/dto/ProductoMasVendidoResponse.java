package com.panaderia.erp.reportes.dto;

import java.math.BigDecimal;

public record ProductoMasVendidoResponse(
        Long productoId,
        String productoNombre,
        BigDecimal cantidadVendida,
        BigDecimal montoTotal
) {
}
