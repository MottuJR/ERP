package com.panaderia.erp.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VentaDiaResponse(
        LocalDate fecha,
        long cantidadVentas,
        BigDecimal totalVendido
) {
}
