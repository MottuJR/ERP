package com.panaderia.erp.compras.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProveedorRequest(
        @NotBlank String nombre,
        String contacto,
        String telefono,
        @Email String email
) {
}
