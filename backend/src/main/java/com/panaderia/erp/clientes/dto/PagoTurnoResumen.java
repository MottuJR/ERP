package com.panaderia.erp.clientes.dto;

import java.math.BigDecimal;

public record PagoTurnoResumen(
        Long cajaId,
        Long usuarioId,
        BigDecimal totalPagado
) {
}
