package com.panaderia.erp.auth.dto;

import com.panaderia.erp.core.usuario.Rol;
import com.panaderia.erp.core.usuario.Usuario;

import java.math.BigDecimal;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        Rol rol,
        boolean activo,
        BigDecimal porcentajeComision
) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.isActivo(),
                usuario.getPorcentajeComision());
    }
}
