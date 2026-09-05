package com.panaderia.erp.clientes.dto;

import com.panaderia.erp.ventas.MedioPago;

import java.math.BigDecimal;
import java.time.Instant;

public record VentaClienteResponse(
        Long id,
        Instant fecha,
        BigDecimal total,
        MedioPago medioPago
) {
}
