package com.panaderia.erp.ventas;

import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Interpreta lo que llega del lector láser en el POS.
 *
 * <p>Sigue la decisión de la sección 5 del documento de diseño: la balanza trabaja en
 * "modo peso" e imprime PLU + peso (sin precio). Para distinguir un código de balanza de
 * un código de barras fijo se usa el rango de prefijo 20-29 (uso interno de comercio en
 * Argentina, EAN-13 de 13 dígitos).
 *
 * <p><b>Importante:</b> el reparto exacto de dígitos entre PLU y peso dentro del código
 * varía según el fabricante de la balanza. Este parseo (5 dígitos de PLU + 5 dígitos de
 * peso en gramos + dígito verificador) es el esquema más común, pero hay que confirmarlo
 * contra el manual del modelo de balanza elegido y ajustar {@code PLU_INICIO}/{@code
 * PLU_FIN}/{@code PESO_INICIO}/{@code PESO_FIN} si hace falta.
 */
@Service
public class EscaneoService {

    private static final int LONGITUD_CODIGO_PESO_VARIABLE = 13;
    private static final int PREFIJO_MIN = 20;
    private static final int PREFIJO_MAX = 29;

    private static final int PLU_INICIO = 2;
    private static final int PLU_FIN = 7;
    private static final int PESO_INICIO = 7;
    private static final int PESO_FIN = 12;

    private final ProductoService productoService;

    public EscaneoService(ProductoService productoService) {
        this.productoService = productoService;
    }

    public ItemResuelto resolver(String codigo, BigDecimal cantidadManual) {
        if (esCodigoPesoVariable(codigo)) {
            return resolverPesoVariable(codigo);
        }

        Producto producto = productoService.obtenerPorCodigoBarras(codigo);
        BigDecimal cantidad = cantidadManual != null ? cantidadManual : BigDecimal.ONE;
        return new ItemResuelto(producto, cantidad);
    }

    boolean esCodigoPesoVariable(String codigo) {
        if (codigo == null
                || codigo.length() != LONGITUD_CODIGO_PESO_VARIABLE
                || !codigo.chars().allMatch(Character::isDigit)) {
            return false;
        }

        int prefijo = Integer.parseInt(codigo.substring(0, 2));
        return prefijo >= PREFIJO_MIN && prefijo <= PREFIJO_MAX;
    }

    private ItemResuelto resolverPesoVariable(String codigo) {
        String plu = codigo.substring(PLU_INICIO, PLU_FIN);
        String pesoEnGramos = codigo.substring(PESO_INICIO, PESO_FIN);

        Producto producto = productoService.obtenerPorCodigoPLU(plu);
        BigDecimal pesoEnKg = new BigDecimal(pesoEnGramos)
                .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);

        return new ItemResuelto(producto, pesoEnKg);
    }

    public record ItemResuelto(Producto producto, BigDecimal cantidad) {
    }
}
