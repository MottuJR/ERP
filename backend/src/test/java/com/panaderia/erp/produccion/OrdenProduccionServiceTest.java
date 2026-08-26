package com.panaderia.erp.produccion;

import com.panaderia.erp.core.exception.ConflictoException;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import com.panaderia.erp.core.usuario.Rol;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import com.panaderia.erp.inventario.InventarioService;
import com.panaderia.erp.produccion.dto.CrearOrdenProduccionRequest;
import com.panaderia.erp.produccion.dto.OrdenProduccionResponse;
import com.panaderia.erp.productos.Categoria;
import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import com.panaderia.erp.productos.TipoProducto;
import com.panaderia.erp.productos.UnidadMedida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenProduccionServiceTest {

    private static final String EMAIL_ENCARGADO = "encargado@panaderia.local";

    @Mock
    private OrdenProduccionRepository ordenProduccionRepository;

    @Mock
    private RecetaRepository recetaRepository;

    @Mock
    private ProductoService productoService;

    @Mock
    private InventarioService inventarioService;

    @Mock
    private UsuarioRepository usuarioRepository;

    private OrdenProduccionService ordenProduccionService;

    @BeforeEach
    void setUp() {
        ordenProduccionService = new OrdenProduccionService(
                ordenProduccionRepository, recetaRepository, productoService, inventarioService, usuarioRepository);
    }

    private void stubUsuarioEncargado() {
        Usuario encargado = new Usuario("Encargada", EMAIL_ENCARGADO, "hash", Rol.ENCARGADO);
        ReflectionTestUtils.setField(encargado, "id", 5L);
        when(usuarioRepository.findByEmail(EMAIL_ENCARGADO)).thenReturn(Optional.of(encargado));
    }

    private void stubGuardarOrdenConId(long id) {
        when(ordenProduccionRepository.save(any(OrdenProduccion.class))).thenAnswer(invocation -> {
            OrdenProduccion orden = invocation.getArgument(0);
            ReflectionTestUtils.setField(orden, "id", id);
            return orden;
        });
    }

    @Test
    void confirmarDescuentaCadaInsumoDeLaRecetaMultiplicadoPorLaCantidadYSumaElStockDelProducto() {
        Producto pan = productoElaborado(1L, "Pan francés");
        Receta receta = new Receta(pan.getId());
        receta.agregarItem(10L, new BigDecimal("0.500")); // 0.5 kg de harina por unidad de pan
        receta.agregarItem(20L, new BigDecimal("0.010")); // 0.01 kg de sal por unidad de pan

        when(productoService.obtenerPorId(1L)).thenReturn(pan);
        when(recetaRepository.findByProductoId(1L)).thenReturn(Optional.of(receta));
        stubUsuarioEncargado();
        stubGuardarOrdenConId(100L);

        CrearOrdenProduccionRequest request = new CrearOrdenProduccionRequest(1L, new BigDecimal("20"));

        OrdenProduccionResponse response = ordenProduccionService.confirmar(request, EMAIL_ENCARGADO);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.cantidad()).isEqualByComparingTo("20");

        // 20 unidades producidas * 0.5 kg de harina = 10 kg
        verify(inventarioService).registrarSalidaInsumoPorProduccion(10L, new BigDecimal("10.000"), 100L);
        // 20 unidades producidas * 0.01 kg de sal = 0.2 kg
        verify(inventarioService).registrarSalidaInsumoPorProduccion(20L, new BigDecimal("0.200"), 100L);
        verify(inventarioService).registrarEntradaProductoPorProduccion(1L, new BigDecimal("20"), 100L);
    }

    @Test
    void siElProductoNoTieneRecetaCargadaFalla() {
        Producto pan = productoElaborado(1L, "Pan francés");
        when(productoService.obtenerPorId(1L)).thenReturn(pan);
        when(recetaRepository.findByProductoId(1L)).thenReturn(Optional.empty());

        CrearOrdenProduccionRequest request = new CrearOrdenProduccionRequest(1L, BigDecimal.TEN);

        assertThatThrownBy(() -> ordenProduccionService.confirmar(request, EMAIL_ENCARGADO))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(ordenProduccionRepository, never()).save(any());
    }

    @Test
    void noSePuedeProducirUnProductoInactivo() {
        Producto pan = productoElaborado(1L, "Pan francés");
        pan.setActivo(false);
        when(productoService.obtenerPorId(1L)).thenReturn(pan);

        CrearOrdenProduccionRequest request = new CrearOrdenProduccionRequest(1L, BigDecimal.TEN);

        assertThatThrownBy(() -> ordenProduccionService.confirmar(request, EMAIL_ENCARGADO))
                .isInstanceOf(ValidacionNegocioException.class);

        verify(recetaRepository, never()).findByProductoId(any());
    }

    @Test
    void siFaltaStockDeUnInsumoLaOrdenPropagaLaExcepcionYNoQuedaConfirmada() {
        Producto pan = productoElaborado(1L, "Pan francés");
        Receta receta = new Receta(pan.getId());
        receta.agregarItem(10L, new BigDecimal("0.500"));

        when(productoService.obtenerPorId(1L)).thenReturn(pan);
        when(recetaRepository.findByProductoId(1L)).thenReturn(Optional.of(receta));
        stubUsuarioEncargado();
        stubGuardarOrdenConId(100L);
        when(inventarioService.registrarSalidaInsumoPorProduccion(eq(10L), any(BigDecimal.class), eq(100L)))
                .thenThrow(new ConflictoException("Stock insuficiente para el insumo \"Harina\""));

        CrearOrdenProduccionRequest request = new CrearOrdenProduccionRequest(1L, new BigDecimal("20"));

        assertThatThrownBy(() -> ordenProduccionService.confirmar(request, EMAIL_ENCARGADO))
                .isInstanceOf(ConflictoException.class);

        verify(inventarioService, never()).registrarEntradaProductoPorProduccion(any(), any(), any());
    }

    private Producto productoElaborado(Long id, String nombre) {
        Producto producto = new Producto(nombre, new Categoria("Panificados"), TipoProducto.ELABORADO,
                false, new BigDecimal("3500.00"), UnidadMedida.UNIDAD, "779" + id, null, BigDecimal.ZERO);
        ReflectionTestUtils.setField(producto, "id", id);
        return producto;
    }
}
