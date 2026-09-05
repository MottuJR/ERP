package com.panaderia.erp.clientes.dto;

import com.panaderia.erp.ventas.MedioPago;

import java.math.BigDecimal;

public record PagoPorMedioPagoResumen(
        MedioPago medioPago,
        BigDecimal total,
        long cantidad
) {
}
