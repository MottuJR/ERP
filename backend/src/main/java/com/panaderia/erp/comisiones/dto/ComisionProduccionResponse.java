package com.panaderia.erp.comisiones.dto;

import java.math.BigDecimal;

public record ComisionProduccionResponse(
        Long ordenId,
        Long usuarioId,
        String usuarioNombre,
        Long productoId,
        String productoNombre,
        BigDecimal cantidadProducida,
        BigDecimal precioProducto,
        BigDecimal porcentaje,
        BigDecimal comision
) {
}
