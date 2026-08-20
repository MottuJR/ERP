package com.panaderia.erp.ventas;

import com.panaderia.erp.core.exception.ConflictoException;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import com.panaderia.erp.core.usuario.Rol;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import com.panaderia.erp.inventario.InventarioService;
import com.panaderia.erp.productos.Categoria;
import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import com.panaderia.erp.productos.TipoProducto;
import com.panaderia.erp.productos.UnidadMedida;
import com.panaderia.erp.ventas.dto.ConfirmarVentaRequest;
import com.panaderia.erp.ventas.dto.ItemVentaRequest;
import com.panaderia.erp.ventas.dto.VentaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    private static final String EMAIL_VENDEDOR = "vendedor@panaderia.local";

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private EscaneoService escaneoService;

    @Mock
    private ProductoService productoService;

    @Mock
    private InventarioService inventarioService;

    @Mock
    private UsuarioRepository usuarioRepository;

    private VentaService ventaService;

    @BeforeEach
    void setUp() {
        ventaService = new VentaService(ventaRepository, escaneoService, productoService, inventarioService, usuarioRepository);

        Usuario vendedor = new Usuario("Vendedora", EMAIL_VENDEDOR, "hash", Rol.VENDEDOR);
        ReflectionTestUtils.setField(vendedor, "id", 1L);
        when(usuarioRepository.findByEmail(EMAIL_VENDEDOR)).thenReturn(java.util.Optional.of(vendedor));
    }

    /**
     * Simula lo que haría la base real: al guardar, le asigna un id a la venta.
     * Se stubea por test (no en setUp) porque no todos los tests llegan a guardar.
     */
    private void stubGuardarVentaConId(long id) {
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> {
            Venta venta = invocation.getArgument(0);
            ReflectionTestUtils.setField(venta, "id", id);
            return venta;
        });
    }

    @Test
    void confirmarVentaConCodigoDeBarrasFijoDescuentaElStockDelProducto() {
        Producto gaseosa = productoFijo(10L, "7791234567890", "Gaseosa 500ml", new BigDecimal("1200.00"));
        when(escaneoService.resolver("7791234567890", null))
                .thenReturn(new EscaneoService.ItemResuelto(gaseosa, BigDecimal.ONE));
        stubGuardarVentaConId(100L);

        ConfirmarVentaRequest request = new ConfirmarVentaRequest(
                null, null, MedioPago.EFECTIVO,
                List.of(new ItemVentaRequest("7791234567890", null, null)));

        VentaResponse response = ventaService.confirmarVenta(request, EMAIL_VENDEDOR);

        assertThat(response.total()).isEqualByComparingTo("1200.00");
        verify(inventarioService).registrarSalidaPorVenta(eq(10L), eq(BigDecimal.ONE), eq(100L));
    }

    @Test
    void confirmarVentaConProductoDePesoVariableCalculaCantidadYDescuentaElPesoExacto() {
        Producto pan = productoDePeso(20L, "12345", "Pan francés", new BigDecimal("3500.00"));
        BigDecimal pesoEnKg = new BigDecimal("0.500");
        when(escaneoService.resolver("2012345005005", null))
                .thenReturn(new EscaneoService.ItemResuelto(pan, pesoEnKg));
        stubGuardarVentaConId(100L);

        ConfirmarVentaRequest request = new ConfirmarVentaRequest(
                null, null, MedioPago.EFECTIVO,
                List.of(new ItemVentaRequest("2012345005005", null, null)));

        VentaResponse response = ventaService.confirmarVenta(request, EMAIL_VENDEDOR);

        // 3500.00 por kg * 0.5 kg = 1750.00
        assertThat(response.total()).isEqualByComparingTo("1750.00");
        assertThat(response.detalles()).hasSize(1);
        assertThat(response.detalles().get(0).cantidad()).isEqualByComparingTo("0.500");
        verify(inventarioService).registrarSalidaPorVenta(eq(20L), eq(pesoEnKg), eq(100L));
    }

    @Test
    void confirmarVentaConVariosItemsSumaElTotalYDescuentaCadaUno() {
        Producto gaseosa = productoFijo(10L, "7791234567890", "Gaseosa 500ml", new BigDecimal("1200.00"));
        Producto pan = productoDePeso(20L, "12345", "Pan francés", new BigDecimal("3500.00"));

        when(escaneoService.resolver("7791234567890", null))
                .thenReturn(new EscaneoService.ItemResuelto(gaseosa, BigDecimal.ONE));
        when(escaneoService.resolver("2012345005005", null))
                .thenReturn(new EscaneoService.ItemResuelto(pan, new BigDecimal("0.500")));
        stubGuardarVentaConId(100L);

        ConfirmarVentaRequest request = new ConfirmarVentaRequest(
                null, null, MedioPago.EFECTIVO,
                List.of(
                        new ItemVentaRequest("7791234567890", null, null),
                        new ItemVentaRequest("2012345005005", null, null)));

        VentaResponse response = ventaService.confirmarVenta(request, EMAIL_VENDEDOR);

        assertThat(response.total()).isEqualByComparingTo("2950.00");
        verify(inventarioService).registrarSalidaPorVenta(eq(10L), eq(BigDecimal.ONE), eq(100L));
        verify(inventarioService).registrarSalidaPorVenta(eq(20L), eq(new BigDecimal("0.500")), eq(100L));
    }

    @Test
    void siNoHayStockSuficienteLaVentaPropagaLaExcepcionYNoQuedaConfirmada() {
        Producto gaseosa = productoFijo(10L, "7791234567890", "Gaseosa 500ml", new BigDecimal("1200.00"));
        when(escaneoService.resolver("7791234567890", null))
                .thenReturn(new EscaneoService.ItemResuelto(gaseosa, BigDecimal.TEN));
        when(inventarioService.registrarSalidaPorVenta(eq(10L), any(BigDecimal.class), anyLong()))
                .thenThrow(new ConflictoException("Stock insuficiente para \"Gaseosa 500ml\": disponible 2, se intentó descontar 10"));
        stubGuardarVentaConId(100L);

        ConfirmarVentaRequest request = new ConfirmarVentaRequest(
                null, null, MedioPago.EFECTIVO,
                List.of(new ItemVentaRequest("7791234567890", null, null)));

        assertThatThrownBy(() -> ventaService.confirmarVenta(request, EMAIL_VENDEDOR))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void unItemConCodigoEscaneadoYProductoIdAlMismoTiempoEsInvalido() {
        ConfirmarVentaRequest request = new ConfirmarVentaRequest(
                null, null, MedioPago.EFECTIVO,
                List.of(new ItemVentaRequest("7791234567890", 10L, BigDecimal.ONE)));

        assertThatThrownBy(() -> ventaService.confirmarVenta(request, EMAIL_VENDEDOR))
                .isInstanceOf(ValidacionNegocioException.class);

        verify(ventaRepository, never()).save(any());
    }

    @Test
    void unItemSinCodigoNiProductoIdEsInvalido() {
        ConfirmarVentaRequest request = new ConfirmarVentaRequest(
                null, null, MedioPago.EFECTIVO,
                List.of(new ItemVentaRequest(null, null, BigDecimal.ONE)));

        assertThatThrownBy(() -> ventaService.confirmarVenta(request, EMAIL_VENDEDOR))
                .isInstanceOf(ValidacionNegocioException.class);
    }

    @Test
    void noSePuedeVenderUnProductoInactivo() {
        Producto gaseosa = productoFijo(10L, "7791234567890", "Gaseosa 500ml", new BigDecimal("1200.00"));
        gaseosa.setActivo(false);
        when(escaneoService.resolver("7791234567890", null))
                .thenReturn(new EscaneoService.ItemResuelto(gaseosa, BigDecimal.ONE));

        ConfirmarVentaRequest request = new ConfirmarVentaRequest(
                null, null, MedioPago.EFECTIVO,
                List.of(new ItemVentaRequest("7791234567890", null, null)));

        assertThatThrownBy(() -> ventaService.confirmarVenta(request, EMAIL_VENDEDOR))
                .isInstanceOf(ValidacionNegocioException.class)
                .hasMessageContaining("no está activo");

        verify(inventarioService, never()).registrarSalidaPorVenta(anyLong(), any(), anyLong());
    }

    private Producto productoFijo(Long id, String codigoBarras, String nombre, BigDecimal precio) {
        Producto producto = new Producto(nombre, categoriaFalsa(), TipoProducto.REVENTA,
                false, precio, UnidadMedida.UNIDAD, codigoBarras, null, BigDecimal.ZERO);
        ReflectionTestUtils.setField(producto, "id", id);
        return producto;
    }

    private Producto productoDePeso(Long id, String plu, String nombre, BigDecimal precioPorKg) {
        Producto producto = new Producto(nombre, categoriaFalsa(), TipoProducto.ELABORADO,
                true, precioPorKg, UnidadMedida.KG, null, plu, BigDecimal.ZERO);
        ReflectionTestUtils.setField(producto, "id", id);
        return producto;
    }

    private Categoria categoriaFalsa() {
        return new Categoria("Panificados");
    }
}
