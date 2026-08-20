package com.panaderia.erp.caja.dto;

import com.panaderia.erp.caja.MovimientoCaja;
import com.panaderia.erp.caja.TipoMovimientoCaja;

import java.math.BigDecimal;
import java.time.Instant;

public record MovimientoCajaResponse(
        Long id,
        Long cajaId,
        TipoMovimientoCaja tipo,
        BigDecimal monto,
        String concepto,
        Instant fecha
) {

    public static MovimientoCajaResponse from(MovimientoCaja movimiento) {
        return new MovimientoCajaResponse(
                movimiento.getId(),
                movimiento.getCajaId(),
                movimiento.getTipo(),
                movimiento.getMonto(),
                movimiento.getConcepto(),
                movimiento.getFecha());
    }
}
