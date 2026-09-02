package com.panaderia.erp.clientes.dto;

import com.panaderia.erp.clientes.Cliente;

public record ClienteResponse(
        Long id,
        String nombre,
        String telefono,
        boolean tieneCuentaCorriente,
        boolean activo
) {

    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(), cliente.getNombre(), cliente.getTelefono(),
                cliente.isTieneCuentaCorriente(), cliente.isActivo());
    }
}
