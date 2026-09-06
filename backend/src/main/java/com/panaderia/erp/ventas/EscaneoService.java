package com.panaderia.erp.ventas;

import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Interpreta lo que llega del lector láser en el POS.
 *
 * <p>Sigue la decisión de la sección 5 del documento de diseño: la balanza imprime PLU +
 * un valor (sin precio). Para distinguir un código de balanza de un código de barras fijo
 * se usa el rango de prefijo 20-29 (uso interno de comercio en Argentina, EAN-13 de 13
 * dígitos), con formato 2-5-5: 2 dígitos de prefijo + 5 de PLU + 5 del valor + dígito
 * verificador.
 *
 * <p>La balanza (confirmado contra el manual de la Kretz Report LT/LT Lite, menú
 * Configuración &gt; Programar código de barras) puede imprimir ese valor de dos formas
 * distintas según el prefijo configurado, porque no todo lo que pasa por la balanza se
 * vende por peso — por ejemplo, facturas se cuentan por unidad aunque se pesen para
 * contarlas más rápido:
 * <ul>
 *   <li>{@link #PREFIJO_PESABLE} (config. de fábrica de la balanza: {@code INI C.B.
 *   PESABLE = 20}, {@code PESO EN C.BARRA = S}): el valor son gramos.</li>
 *   <li>{@link #PREFIJO_UNIDADES} ({@code INI C.BARRA UNI = 21}, {@code UNID EN C.BARRA
 *   = S}): el valor es directamente una cantidad de unidades, sin dividir.</li>
 * </ul>
 * Estos dos prefijos y el formato 2-5-5 son el acuerdo con el que hay que configurar la
 * balanza real cuando esté disponible; si en la balanza real se terminan usando otros
 * valores, alcanza con ajustar {@code PREFIJO_PESABLE}/{@code PREFIJO_UNIDADES} acá.
 */
@Service
public class EscaneoService {

    private static final int LONGITUD_CODIGO_PESO_VARIABLE = 13;
    private static final int PREFIJO_MIN = 20;
    private static final int PREFIJO_MAX = 29;

    private static final int PREFIJO_PESABLE = 20;
    private static final int PREFIJO_UNIDADES = 21;

    private static final int PLU_INICIO = 2;
    private static final int PLU_FIN = 7;
    private static final int VALOR_INICIO = 7;
    private static final int VALOR_FIN = 12;

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
        int prefijo = Integer.parseInt(codigo.substring(0, 2));
        String plu = codigo.substring(PLU_INICIO, PLU_FIN);
        String valor = codigo.substring(VALOR_INICIO, VALOR_FIN);

        Producto producto = productoService.obtenerPorCodigoPLU(plu);
        BigDecimal cantidad = prefijo == PREFIJO_UNIDADES
                ? new BigDecimal(valor)
                : new BigDecimal(valor).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);

        return new ItemResuelto(producto, cantidad);
    }

    public record ItemResuelto(Producto producto, BigDecimal cantidad) {
    }
}
