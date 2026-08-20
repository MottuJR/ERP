package com.panaderia.erp.productos.dto;

import com.panaderia.erp.productos.Categoria;

public record CategoriaResponse(
        Long id,
        String nombre
) {

    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre());
    }
}
