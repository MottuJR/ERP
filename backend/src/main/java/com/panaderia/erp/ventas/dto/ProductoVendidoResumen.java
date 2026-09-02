package com.panaderia.erp.ventas.dto;

import java.math.BigDecimal;

public record ProductoVendidoResumen(
        Long productoId,
        BigDecimal cantidadVendida,
        BigDecimal montoTotal
) {
}
