package com.panaderia.erp.reportes.dto;

import com.panaderia.erp.ventas.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ReporteIngresosResponse(
        LocalDate desde,
        LocalDate hasta,
        List<IngresoDiaResponse> porDia,
        Map<MedioPago, BigDecimal> totalesPorMedioPago
) {
}
