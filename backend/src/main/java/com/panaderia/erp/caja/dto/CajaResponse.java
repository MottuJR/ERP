package com.panaderia.erp.caja.dto;

import com.panaderia.erp.caja.Caja;
import com.panaderia.erp.caja.EstadoCaja;

import java.math.BigDecimal;
import java.time.Instant;

public record CajaResponse(
        Long id,
        Instant fechaApertura,
        Instant fechaCierre,
        BigDecimal montoInicial,
        BigDecimal montoFinal,
        Long usuarioId,
        EstadoCaja estado
) {

    public static CajaResponse from(Caja caja) {
        return new CajaResponse(
                caja.getId(),
                caja.getFechaApertura(),
                caja.getFechaCierre(),
                caja.getMontoInicial(),
                caja.getMontoFinal(),
                caja.getUsuarioId(),
                caja.getEstado());
    }
}
