package com.panaderia.erp.ventas.dto;

import com.panaderia.erp.ventas.MedioPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ConfirmarVentaRequest(
        Long clienteId,
        Long cajaId,
        @NotNull MedioPago medioPago,
        @NotEmpty @Valid List<ItemVentaRequest> items
) {
}
