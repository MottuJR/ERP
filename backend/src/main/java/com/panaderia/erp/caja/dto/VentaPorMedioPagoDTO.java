package com.panaderia.erp.caja.dto;

import com.panaderia.erp.clientes.dto.PagoPorMedioPagoResumen;
import com.panaderia.erp.ventas.MedioPago;
import com.panaderia.erp.ventas.dto.VentaPorMedioPagoResumen;

import java.math.BigDecimal;

public record VentaPorMedioPagoDTO(
        MedioPago medioPago,
        BigDecimal total,
        long cantidad
) {

    public static VentaPorMedioPagoDTO from(VentaPorMedioPagoResumen resumen) {
        return new VentaPorMedioPagoDTO(resumen.medioPago(), resumen.total(), resumen.cantidad());
    }

    public static VentaPorMedioPagoDTO fromPago(PagoPorMedioPagoResumen resumen) {
        return new VentaPorMedioPagoDTO(resumen.medioPago(), resumen.total(), resumen.cantidad());
    }
}
