package com.panaderia.erp.compras.dto;

import com.panaderia.erp.compras.Proveedor;

public record ProveedorResponse(
        Long id,
        String nombre,
        String contacto,
        String telefono,
        String email,
        boolean activo
) {

    public static ProveedorResponse from(Proveedor proveedor) {
        return new ProveedorResponse(
                proveedor.getId(), proveedor.getNombre(), proveedor.getContacto(),
                proveedor.getTelefono(), proveedor.getEmail(), proveedor.isActivo());
    }
}
