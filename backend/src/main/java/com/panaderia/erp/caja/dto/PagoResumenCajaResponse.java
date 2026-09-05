package com.panaderia.erp.caja.dto;

import com.panaderia.erp.ventas.MedioPago;

import java.math.BigDecimal;
import java.time.Instant;

public record PagoResumenCajaResponse(
        Long id,
        Instant fecha,
        MedioPago medioPago,
        BigDecimal monto,
        String usuarioNombre,
        String clienteNombre
) {
}
