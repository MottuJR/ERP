package com.panaderia.erp.produccion.dto;

import com.panaderia.erp.produccion.EstadoOrdenProduccion;

import java.math.BigDecimal;
import java.time.Instant;

public record OrdenProduccionResponse(
        Long id,
        Long productoId,
        String productoNombre,
        BigDecimal cantidad,
        Instant fecha,
        EstadoOrdenProduccion estado,
        Long usuarioId
) {
}
