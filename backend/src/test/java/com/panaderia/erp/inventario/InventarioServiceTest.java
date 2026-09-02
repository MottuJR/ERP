package com.panaderia.erp.inventario;

import com.panaderia.erp.core.auditoria.AccionAuditoria;
import com.panaderia.erp.core.auditoria.AuditoriaService;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import com.panaderia.erp.inventario.dto.InsumoRequest;
import com.panaderia.erp.inventario.dto.MovimientoManualRequest;
import com.panaderia.erp.productos.ProductoService;
import com.panaderia.erp.productos.UnidadMedida;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    private static final String EMAIL = "encargada@panaderia.local";

    @Mock
    private InsumoRepository insumoRepository;

    @Mock
    private MovimientoStockRepository movimientoStockRepository;

    @Mock
    private ProductoService productoService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private InventarioService inventarioService;

    private Insumo insumo(long id, BigDecimal stockActual) {
        Insumo insumo = new Insumo("Harina", UnidadMedida.KG, BigDecimal.ZERO, BigDecimal.ZERO);
        ReflectionTestUtils.setField(insumo, "id", id);
        ReflectionTestUtils.setField(insumo, "stockActual", stockActual);
        return insumo;
    }

    @Test
    void actualizarInsumoCambiaLosDatosMaestrosSinTocarElStockActual() {
        Insumo harina = insumo(1L, new BigDecimal("20.000"));
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(harina));

        InsumoRequest request = new InsumoRequest(
                "Harina 000", UnidadMedida.KG, new BigDecimal("15.000"), new BigDecimal("650.00"));

        Insumo actualizado = inventarioService.actualizarInsumo(1L, request);

        assertThat(actualizado.getNombre()).isEqualTo("Harina 000");
        assertThat(actualizado.getStockMinimo()).isEqualByComparingTo("15.000");
        assertThat(actualizado.getCostoUnitario()).isEqualByComparingTo("650.00");
        assertThat(actualizado.getStockActual()).isEqualByComparingTo("20.000");
    }

    @Test
    void unAjusteManualDeEntradaDeInsumoSumaStockYRegistraAuditoria() {
        Insumo harina = insumo(1L, new BigDecimal("10.000"));
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(harina));
        when(movimientoStockRepository.save(any(MovimientoStock.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoManualRequest request = new MovimientoManualRequest(
                ItemTipo.INSUMO, 1L, TipoMovimiento.ENTRADA, new BigDecimal("5.000"), "Conteo físico");

        inventarioService.registrarMovimientoManual(request, EMAIL);

        assertThat(harina.getStockActual()).isEqualByComparingTo("15.000");
        verify(auditoriaService).registrar(eq(EMAIL), eq("INSUMO"), eq(1L), eq(AccionAuditoria.AJUSTE_STOCK), any());
    }

    @Test
    void unAjusteManualDeSalidaDeProductoDescuentaViaProductoService() {
        when(movimientoStockRepository.save(any(MovimientoStock.class))).thenAnswer(inv -> inv.getArgument(0));

        MovimientoManualRequest request = new MovimientoManualRequest(
                ItemTipo.PRODUCTO, 2L, TipoMovimiento.SALIDA, new BigDecimal("-3.000"), "Producto vencido");

        inventarioService.registrarMovimientoManual(request, EMAIL);

        verify(productoService).ajustarStockActual(2L, new BigDecimal("-3.000"));
        verify(auditoriaService).registrar(eq(EMAIL), eq("PRODUCTO"), eq(2L), eq(AccionAuditoria.AJUSTE_STOCK), any());
    }

    @Test
    void unMovimientoDeEntradaConCantidadNegativaEsInvalido() {
        MovimientoManualRequest request = new MovimientoManualRequest(
                ItemTipo.INSUMO, 1L, TipoMovimiento.ENTRADA, new BigDecimal("-5.000"), "Motivo");

        assertThatThrownBy(() -> inventarioService.registrarMovimientoManual(request, EMAIL))
                .isInstanceOf(ValidacionNegocioException.class);

        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void unMovimientoConCantidadCeroEsInvalido() {
        MovimientoManualRequest request = new MovimientoManualRequest(
                ItemTipo.INSUMO, 1L, TipoMovimiento.AJUSTE, BigDecimal.ZERO, "Motivo");

        assertThatThrownBy(() -> inventarioService.registrarMovimientoManual(request, EMAIL))
                .isInstanceOf(ValidacionNegocioException.class);

        verify(insumoRepository, never()).findById(any());
    }
}
