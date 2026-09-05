package com.panaderia.erp.caja.dto;

import com.panaderia.erp.ventas.MedioPago;

import java.math.BigDecimal;
import java.time.Instant;

public record VentaResumenCajaResponse(
        Long id,
        Instant fecha,
        MedioPago medioPago,
        BigDecimal total,
        String usuarioNombre,
        String clienteNombre
) {
}
