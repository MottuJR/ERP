package com.panaderia.erp.ventas.dto;

import java.math.BigDecimal;

/**
 * Un renglón del carrito. O trae {@code codigoEscaneado} (lo que devolvió el lector láser,
 * sea código de barras fijo o etiqueta de balanza), o trae {@code productoId} +
 * {@code cantidad} para el caso de agregar un producto buscándolo manualmente (sin scanner).
 * Exactamente una de las dos formas debe estar presente; se valida en {@code VentaService}.
 */
public record ItemVentaRequest(
        String codigoEscaneado,
        Long productoId,
        BigDecimal cantidad
) {
}
