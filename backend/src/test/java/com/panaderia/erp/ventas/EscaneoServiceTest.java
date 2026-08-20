package com.panaderia.erp.ventas;

import com.panaderia.erp.productos.Categoria;
import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import com.panaderia.erp.productos.TipoProducto;
import com.panaderia.erp.productos.UnidadMedida;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscaneoServiceTest {

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private EscaneoService escaneoService;

    @Test
    void unCodigoDe13DigitosConPrefijo20A29EsPesoVariable() {
        assertThat(escaneoService.esCodigoPesoVariable("2012345005005")).isTrue();
    }

    @Test
    void unCodigoConPrefijoFueraDelRango20A29NoEsPesoVariable() {
        assertThat(escaneoService.esCodigoPesoVariable("7791234567890")).isFalse();
    }

    @Test
    void unCodigoQueNoTiene13DigitosNoEsPesoVariable() {
        assertThat(escaneoService.esCodigoPesoVariable("2012345")).isFalse();
    }

    @Test
    void resuelveUnCodigoDePesoVariableCalculandoElPesoEnKgYBuscandoPorPLU() {
        // prefijo 20 + PLU 12345 + 00500 gramos + dígito verificador
        String codigoBalanza = "2012345005005";
        Producto pan = productoDePeso("12345", "Pan francés", new BigDecimal("3500.00"));
        when(productoService.obtenerPorCodigoPLU("12345")).thenReturn(pan);

        EscaneoService.ItemResuelto resuelto = escaneoService.resolver(codigoBalanza, null);

        assertThat(resuelto.producto()).isEqualTo(pan);
        assertThat(resuelto.cantidad()).isEqualByComparingTo("0.500");
        verify(productoService, never()).obtenerPorCodigoBarras(anyString());
    }

    @Test
    void resuelveUnCodigoDeBarrasFijoConCantidadUnoPorDefecto() {
        String codigoBarras = "7791234567890";
        Producto gaseosa = productoFijo(codigoBarras, "Gaseosa 500ml", new BigDecimal("1200.00"));
        when(productoService.obtenerPorCodigoBarras(codigoBarras)).thenReturn(gaseosa);

        EscaneoService.ItemResuelto resuelto = escaneoService.resolver(codigoBarras, null);

        assertThat(resuelto.producto()).isEqualTo(gaseosa);
        assertThat(resuelto.cantidad()).isEqualByComparingTo("1");
    }

    @Test
    void resuelveUnCodigoDeBarrasFijoRespetandoLaCantidadManual() {
        String codigoBarras = "7791234567890";
        Producto gaseosa = productoFijo(codigoBarras, "Gaseosa 500ml", new BigDecimal("1200.00"));
        when(productoService.obtenerPorCodigoBarras(codigoBarras)).thenReturn(gaseosa);

        EscaneoService.ItemResuelto resuelto = escaneoService.resolver(codigoBarras, new BigDecimal("3"));

        assertThat(resuelto.cantidad()).isEqualByComparingTo("3");
    }

    private Producto productoDePeso(String plu, String nombre, BigDecimal precioPorKg) {
        return new Producto(nombre, categoriaFalsa(), TipoProducto.ELABORADO,
                true, precioPorKg, UnidadMedida.KG, null, plu, BigDecimal.ZERO);
    }

    private Producto productoFijo(String codigoBarras, String nombre, BigDecimal precio) {
        return new Producto(nombre, categoriaFalsa(), TipoProducto.REVENTA,
                false, precio, UnidadMedida.UNIDAD, codigoBarras, null, BigDecimal.ZERO);
    }

    private Categoria categoriaFalsa() {
        return new Categoria("Panificados");
    }
}
