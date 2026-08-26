package com.panaderia.erp.compras.dto;

import com.panaderia.erp.compras.EstadoCompra;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CompraResponse(
        Long id,
        Long proveedorId,
        String proveedorNombre,
        Instant fecha,
        BigDecimal total,
        EstadoCompra estado,
        List<DetalleCompraResponse> detalles
) {
}
