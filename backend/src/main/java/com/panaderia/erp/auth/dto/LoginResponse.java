package com.panaderia.erp.auth.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresInMinutes,
        UsuarioResponse usuario
) {
}
