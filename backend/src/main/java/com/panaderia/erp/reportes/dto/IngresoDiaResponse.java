package com.panaderia.erp.reportes.dto;

import com.panaderia.erp.ventas.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record IngresoDiaResponse(
        LocalDate fecha,
        Map<MedioPago, BigDecimal> porMedioPago,
        BigDecimal total
) {
}
