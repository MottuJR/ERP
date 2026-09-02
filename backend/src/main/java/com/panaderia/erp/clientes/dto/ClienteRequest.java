package com.panaderia.erp.clientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClienteRequest(
        @NotBlank String nombre,
        String telefono,
        @NotNull Boolean tieneCuentaCorriente
) {
}
