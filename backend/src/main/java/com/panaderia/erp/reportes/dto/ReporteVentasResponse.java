package com.panaderia.erp.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteVentasResponse(
        LocalDate desde,
        LocalDate hasta,
        long cantidadVentas,
        BigDecimal totalVendido,
        BigDecimal promedioPorVenta,
        List<VentaDiaResponse> porDia
) {
}
