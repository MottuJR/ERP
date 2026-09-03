package com.panaderia.erp.auth.dto;

import com.panaderia.erp.core.usuario.Rol;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// password es opcional: si viene null o en blanco, se deja la contraseña actual sin tocar.
public record ActualizarUsuarioRequest(
        @NotBlank String nombre,
        @NotBlank @Email String email,
        @NotNull Rol rol,
        boolean activo,
        @DecimalMin(value = "0.0") @DecimalMax(value = "100.0") BigDecimal porcentajeComision,
        @Size(min = 8) String password
) {
}
