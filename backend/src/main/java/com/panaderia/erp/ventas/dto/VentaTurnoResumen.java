package com.panaderia.erp.ventas.dto;

import java.math.BigDecimal;

public record VentaTurnoResumen(
        Long cajaId,
        Long usuarioId,
        BigDecimal totalVendido
) {
}
