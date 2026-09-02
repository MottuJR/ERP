package com.panaderia.erp.clientes.dto;

import java.math.BigDecimal;

public record SaldoClienteResponse(
        Long clienteId,
        String clienteNombre,
        BigDecimal totalVentasCuentaCorriente,
        BigDecimal totalPagos,
        BigDecimal saldo
) {
}
