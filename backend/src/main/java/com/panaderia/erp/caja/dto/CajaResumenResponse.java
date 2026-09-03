package com.panaderia.erp.caja.dto;

import com.panaderia.erp.caja.EstadoCaja;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CajaResumenResponse(
        Long id,
        Instant fechaApertura,
        Instant fechaCierre,
        BigDecimal montoInicial,
        BigDecimal montoFinal,
        Long usuarioId,
        String usuarioNombre,
        EstadoCaja estado,
        List<VentaPorMedioPagoDTO> ventasPorMedioPago,
        BigDecimal totalVentas,
        BigDecimal totalIngresos,
        BigDecimal totalEgresos,
        BigDecimal efectivoEsperado,
        BigDecimal diferencia
) {
}
