package com.panaderia.erp.productos;

import com.panaderia.erp.core.auditoria.AccionAuditoria;
import com.panaderia.erp.core.auditoria.AuditoriaService;
import com.panaderia.erp.core.exception.ConflictoException;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import com.panaderia.erp.productos.dto.ActualizarProductoRequest;
import com.panaderia.erp.productos.dto.CrearProductoRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    private static final String EMAIL = "encargada@panaderia.local";

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ProductoService productoService;

    private Categoria categoria() {
        Categoria categoria = new Categoria("Panificados");
        ReflectionTestUtils.setField(categoria, "id", 1L);
        return categoria;
    }

    private CrearProductoRequest requestValido() {
        return new CrearProductoRequest("Gaseosa", 1L, TipoProducto.REVENTA, false,
                new BigDecimal("1200.00"), UnidadMedida.UNIDAD, "7790000000001", null, BigDecimal.ZERO);
    }

    @Test
    void crearGuardaElProductoYRegistraAuditoriaDeAlta() {
        when(categoriaService.obtenerPorId(1L)).thenReturn(categoria());
        when(productoRepository.findByCodigoBarrasConCategoria(any())).thenReturn(Optional.empty());
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 10L);
            return p;
        });

        Producto producto = productoService.crear(requestValido(), EMAIL);

        assertThat(producto.getId()).isEqualTo(10L);
        verify(auditoriaService).registrar(eq(EMAIL), eq("Producto"), eq(10L), eq(AccionAuditoria.CREAR), any());
    }

    @Test
    void unProductoVendidoPorPesoSinCodigoPLUFalla() {
        CrearProductoRequest request = new CrearProductoRequest("Pan", 1L, TipoProducto.ELABORADO, true,
                new BigDecimal("3500.00"), UnidadMedida.KG, null, null, BigDecimal.ZERO);

        assertThatThrownBy(() -> productoService.crear(request, EMAIL))
                .isInstanceOf(ValidacionNegocioException.class);

        verify(productoRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void unProductoNoVendidoPorPesoPuedeTenerCodigoPLU() {
        // Ej.: facturas, contadas por unidad pero pasadas por la balanza en modo "unidades" en
        // vez de peso (ver EscaneoService) — no es exclusivo de productos vendidos por peso.
        CrearProductoRequest request = new CrearProductoRequest("Facturas", 1L, TipoProducto.ELABORADO, false,
                new BigDecimal("800.00"), UnidadMedida.UNIDAD, null, "54321", BigDecimal.ZERO);
        when(categoriaService.obtenerPorId(1L)).thenReturn(categoria());
        when(productoRepository.findByCodigoPLUConCategoria("54321")).thenReturn(Optional.empty());
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto producto = productoService.crear(request, EMAIL);

        assertThat(producto.getCodigoPLU()).isEqualTo("54321");
    }

    @Test
    void noSePuedeCrearUnProductoConCodigoDeBarrasYaUsado() {
        when(productoRepository.findByCodigoBarrasConCategoria("7790000000001"))
                .thenReturn(Optional.of(productoExistente(99L, "7790000000001")));

        assertThatThrownBy(() -> productoService.crear(requestValido(), EMAIL))
                .isInstanceOf(ConflictoException.class);

        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void actualizarConCambioDePrecioRegistraElPrecioAnteriorYElNuevoEnElDetalle() {
        Producto existente = productoExistente(10L, "7790000000001");
        existente.setPrecioVenta(new BigDecimal("1000.00"));
        when(productoRepository.findByIdConCategoria(10L)).thenReturn(Optional.of(existente));
        when(productoRepository.findByCodigoBarrasConCategoria("7790000000001"))
                .thenReturn(Optional.of(existente));
        when(categoriaService.obtenerPorId(1L)).thenReturn(categoria());

        ActualizarProductoRequest request = new ActualizarProductoRequest(
                "Gaseosa", 1L, TipoProducto.REVENTA, false, new BigDecimal("1300.00"),
                UnidadMedida.UNIDAD, "7790000000001", null, BigDecimal.ZERO, true);

        productoService.actualizar(10L, request, EMAIL);

        verify(auditoriaService).registrar(
                eq(EMAIL), eq("Producto"), eq(10L), eq(AccionAuditoria.ACTUALIZAR),
                contains("1000.00 -> 1300.00"));
    }

    @Test
    void actualizarSinCambioDePrecioNoMencionaPrecioEnElDetalle() {
        Producto existente = productoExistente(10L, "7790000000001");
        existente.setPrecioVenta(new BigDecimal("1200.00"));
        when(productoRepository.findByIdConCategoria(10L)).thenReturn(Optional.of(existente));
        when(productoRepository.findByCodigoBarrasConCategoria("7790000000001"))
                .thenReturn(Optional.of(existente));
        when(categoriaService.obtenerPorId(1L)).thenReturn(categoria());

        ActualizarProductoRequest request = new ActualizarProductoRequest(
                "Gaseosa 500ml", 1L, TipoProducto.REVENTA, false, new BigDecimal("1200.00"),
                UnidadMedida.UNIDAD, "7790000000001", null, BigDecimal.ZERO, true);

        productoService.actualizar(10L, request, EMAIL);

        verify(auditoriaService).registrar(
                eq(EMAIL), eq("Producto"), eq(10L), eq(AccionAuditoria.ACTUALIZAR),
                argThat((String detalle) -> !detalle.contains("Cambio de precio")));
    }

    @Test
    void desactivarMarcaElProductoInactivoYRegistraAuditoria() {
        Producto existente = productoExistente(10L, "7790000000001");
        when(productoRepository.findByIdConCategoria(10L)).thenReturn(Optional.of(existente));

        productoService.desactivar(10L, EMAIL);

        assertThat(existente.isActivo()).isFalse();
        verify(auditoriaService).registrar(eq(EMAIL), eq("Producto"), eq(10L), eq(AccionAuditoria.DESACTIVAR), any());
    }

    private Producto productoExistente(Long id, String codigoBarras) {
        Producto producto = new Producto("Gaseosa", categoria(), TipoProducto.REVENTA, false,
                new BigDecimal("1200.00"), UnidadMedida.UNIDAD, codigoBarras, null, BigDecimal.ZERO);
        ReflectionTestUtils.setField(producto, "id", id);
        return producto;
    }
}
