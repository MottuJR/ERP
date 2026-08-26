package com.panaderia.erp.compras;

import com.panaderia.erp.compras.dto.CompraResponse;
import com.panaderia.erp.compras.dto.ConfirmarCompraRequest;
import com.panaderia.erp.compras.dto.ItemCompraRequest;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.inventario.Insumo;
import com.panaderia.erp.inventario.InventarioService;
import com.panaderia.erp.productos.UnidadMedida;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private ProveedorService proveedorService;

    @Mock
    private InventarioService inventarioService;

    private CompraService compraService;

    @BeforeEach
    void setUp() {
        compraService = new CompraService(compraRepository, proveedorService, inventarioService);
    }

    private void stubGuardarCompraConId(long id) {
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocation -> {
            Compra compra = invocation.getArgument(0);
            ReflectionTestUtils.setField(compra, "id", id);
            return compra;
        });
    }

    private Proveedor proveedor(long id, String nombre) {
        Proveedor proveedor = new Proveedor(nombre, "Contacto", "1234", "proveedor@mail.com");
        ReflectionTestUtils.setField(proveedor, "id", id);
        return proveedor;
    }

    private Insumo insumo(long id, String nombre, BigDecimal costoActual) {
        Insumo insumo = new Insumo(nombre, UnidadMedida.KG, BigDecimal.ZERO, costoActual);
        ReflectionTestUtils.setField(insumo, "id", id);
        return insumo;
    }

    @Test
    void confirmarCompraSumaStockYActualizaElCostoUnitarioDelInsumo() {
        Proveedor proveedor = proveedor(1L, "Molino SA");
        Insumo harina = insumo(10L, "Harina", new BigDecimal("500.00"));

        when(proveedorService.obtenerPorId(1L)).thenReturn(proveedor);
        when(inventarioService.obtenerInsumoPorId(10L)).thenReturn(harina);
        stubGuardarCompraConId(200L);

        ConfirmarCompraRequest request = new ConfirmarCompraRequest(
                1L, List.of(new ItemCompraRequest(10L, new BigDecimal("50"), new BigDecimal("550.00"))));

        compraService.confirmarCompra(request);

        verify(inventarioService).registrarEntradaInsumoPorCompra(
                10L, new BigDecimal("50"), new BigDecimal("550.00"), 200L);
    }

    @Test
    void confirmarCompraConVariosItemsCalculaElTotalCorrectamente() {
        Proveedor proveedor = proveedor(1L, "Molino SA");
        Insumo harina = insumo(10L, "Harina", new BigDecimal("500.00"));
        Insumo levadura = insumo(11L, "Levadura", new BigDecimal("1200.00"));

        when(proveedorService.obtenerPorId(1L)).thenReturn(proveedor);
        when(inventarioService.obtenerInsumoPorId(10L)).thenReturn(harina);
        when(inventarioService.obtenerInsumoPorId(11L)).thenReturn(levadura);
        stubGuardarCompraConId(200L);

        ConfirmarCompraRequest request = new ConfirmarCompraRequest(
                1L, List.of(
                        new ItemCompraRequest(10L, new BigDecimal("50"), new BigDecimal("550.00")),
                        new ItemCompraRequest(11L, new BigDecimal("5"), new BigDecimal("1300.00"))));

        CompraResponse response = compraService.confirmarCompra(request);

        // 50 * 550.00 = 27500.00 ; 5 * 1300.00 = 6500.00 ; total = 34000.00
        assertThat(response.total()).isEqualByComparingTo("34000.00");
        assertThat(response.detalles()).hasSize(2);
        verify(inventarioService).registrarEntradaInsumoPorCompra(10L, new BigDecimal("50"), new BigDecimal("550.00"), 200L);
        verify(inventarioService).registrarEntradaInsumoPorCompra(11L, new BigDecimal("5"), new BigDecimal("1300.00"), 200L);
    }

    @Test
    void siElProveedorNoExisteNoSeRegistraNadaEnInventario() {
        when(proveedorService.obtenerPorId(1L)).thenThrow(new RecursoNoEncontradoException("Proveedor no encontrado: 1"));

        ConfirmarCompraRequest request = new ConfirmarCompraRequest(
                1L, List.of(new ItemCompraRequest(10L, BigDecimal.TEN, BigDecimal.TEN)));

        assertThatThrownBy(() -> compraService.confirmarCompra(request))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(compraRepository, never()).save(any());
        verify(inventarioService, never()).registrarEntradaInsumoPorCompra(any(), any(), any(), any());
    }

    @Test
    void siUnInsumoDelDetalleNoExisteNoSeGuardaLaCompra() {
        Proveedor proveedor = proveedor(1L, "Molino SA");
        when(proveedorService.obtenerPorId(1L)).thenReturn(proveedor);
        when(inventarioService.obtenerInsumoPorId(999L))
                .thenThrow(new RecursoNoEncontradoException("Insumo no encontrado: 999"));

        ConfirmarCompraRequest request = new ConfirmarCompraRequest(
                1L, List.of(new ItemCompraRequest(999L, BigDecimal.TEN, BigDecimal.TEN)));

        assertThatThrownBy(() -> compraService.confirmarCompra(request))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(compraRepository, never()).save(any());
    }
}
