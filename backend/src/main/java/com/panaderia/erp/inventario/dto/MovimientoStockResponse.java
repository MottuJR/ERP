package com.panaderia.erp.inventario.dto;

import com.panaderia.erp.inventario.ItemTipo;
import com.panaderia.erp.inventario.MovimientoStock;
import com.panaderia.erp.inventario.TipoMovimiento;

import java.math.BigDecimal;
import java.time.Instant;

public record MovimientoStockResponse(
        Long id,
        TipoMovimiento tipo,
        ItemTipo itemTipo,
        Long itemId,
        BigDecimal cantidad,
        Instant fecha,
        String motivo,
        Long referenciaId
) {

    public static MovimientoStockResponse from(MovimientoStock movimiento) {
        return new MovimientoStockResponse(
                movimiento.getId(),
                movimiento.getTipo(),
                movimiento.getItemTipo(),
                movimiento.getItemId(),
                movimiento.getCantidad(),
                movimiento.getFecha(),
                movimiento.getMotivo(),
                movimiento.getReferenciaId());
    }
}
