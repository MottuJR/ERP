package com.panaderia.erp.caja.dto;

import com.panaderia.erp.caja.Caja;
import com.panaderia.erp.caja.EstadoCaja;

import java.math.BigDecimal;
import java.time.Instant;

public record CajaHistorialResponse(
        Long id,
        Instant fechaApertura,
        Instant fechaCierre,
        BigDecimal montoInicial,
        BigDecimal montoFinal,
        Long usuarioId,
        String usuarioNombre,
        EstadoCaja estado
) {

    public static CajaHistorialResponse from(Caja caja, String usuarioNombre) {
        return new CajaHistorialResponse(
                caja.getId(),
                caja.getFechaApertura(),
                caja.getFechaCierre(),
                caja.getMontoInicial(),
                caja.getMontoFinal(),
                caja.getUsuarioId(),
                usuarioNombre,
                caja.getEstado());
    }
}
