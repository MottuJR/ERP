package com.panaderia.erp.comisiones.dto;

import java.math.BigDecimal;

public record ComisionVendedorResponse(
        Long cajaId,
        Long usuarioId,
        String usuarioNombre,
        BigDecimal totalVendido,
        BigDecimal porcentaje,
        BigDecimal comision
) {
}
