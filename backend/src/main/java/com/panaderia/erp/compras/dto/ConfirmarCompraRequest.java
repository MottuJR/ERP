package com.panaderia.erp.compras.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ConfirmarCompraRequest(
        @NotNull Long proveedorId,
        @NotEmpty @Valid List<ItemCompraRequest> items
) {
}
