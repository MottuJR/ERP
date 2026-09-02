package com.panaderia.erp.clientes.dto;

import com.panaderia.erp.clientes.PagoCliente;
import com.panaderia.erp.ventas.MedioPago;

import java.math.BigDecimal;
import java.time.Instant;

public record PagoClienteResponse(
        Long id,
        Long clienteId,
        Instant fecha,
        BigDecimal monto,
        MedioPago medioPago
) {

    public static PagoClienteResponse from(PagoCliente pago) {
        return new PagoClienteResponse(
                pago.getId(), pago.getClienteId(), pago.getFecha(), pago.getMonto(), pago.getMedioPago());
    }
}
