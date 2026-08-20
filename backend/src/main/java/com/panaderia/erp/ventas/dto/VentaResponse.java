package com.panaderia.erp.ventas.dto;

import com.panaderia.erp.ventas.EstadoVenta;
import com.panaderia.erp.ventas.MedioPago;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record VentaResponse(
        Long id,
        Instant fecha,
        Long clienteId,
        Long usuarioId,
        Long cajaId,
        BigDecimal total,
        MedioPago medioPago,
        EstadoVenta estado,
        List<DetalleVentaResponse> detalles
) {
}
