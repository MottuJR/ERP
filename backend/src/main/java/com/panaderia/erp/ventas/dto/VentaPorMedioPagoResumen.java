package com.panaderia.erp.ventas.dto;

import com.panaderia.erp.ventas.MedioPago;

import java.math.BigDecimal;

public record VentaPorMedioPagoResumen(
        MedioPago medioPago,
        BigDecimal total,
        long cantidad
) {
}
