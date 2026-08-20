package com.panaderia.erp.ventas.dto;

import java.math.BigDecimal;

public record EscaneoResponse(
        Long productoId,
        String productoNombre,
        boolean seVendePorPeso,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
