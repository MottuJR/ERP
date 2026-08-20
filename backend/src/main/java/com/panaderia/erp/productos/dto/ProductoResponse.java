package com.panaderia.erp.productos.dto;

import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.TipoProducto;
import com.panaderia.erp.productos.UnidadMedida;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String nombre,
        Long categoriaId,
        String categoriaNombre,
        TipoProducto tipo,
        boolean seVendePorPeso,
        BigDecimal precioVenta,
        UnidadMedida unidadMedida,
        String codigoBarras,
        String codigoPLU,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        boolean activo
) {

    public static ProductoResponse from(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                producto.getTipo(),
                producto.isSeVendePorPeso(),
                producto.getPrecioVenta(),
                producto.getUnidadMedida(),
                producto.getCodigoBarras(),
                producto.getCodigoPLU(),
                producto.getStockActual(),
                producto.getStockMinimo(),
                producto.isActivo());
    }
}
