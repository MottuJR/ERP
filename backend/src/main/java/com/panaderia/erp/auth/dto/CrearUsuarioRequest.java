package com.panaderia.erp.auth.dto;

import com.panaderia.erp.core.usuario.Rol;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CrearUsuarioRequest(
        @NotBlank String nombre,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotNull Rol rol,
        @DecimalMin(value = "0.0") @DecimalMax(value = "100.0") BigDecimal porcentajeComision
) {
}
