package com.panaderia.erp.reportes;

import com.panaderia.erp.clientes.CuentaCorrienteService;
import com.panaderia.erp.clientes.PagoCliente;
import com.panaderia.erp.reportes.dto.ReporteIngresosResponse;
import com.panaderia.erp.ventas.MedioPago;
import com.panaderia.erp.ventas.Venta;
import com.panaderia.erp.ventas.VentaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportesServiceTest {

    @Mock
    private VentaService ventaService;

    @Mock
    private CuentaCorrienteService cuentaCorrienteService;

    @InjectMocks
    private ReportesService reportesService;

    private Venta venta(MedioPago medioPago, String total, Instant fecha) {
        Venta venta = new Venta(null, 1L, 1L, medioPago);
        ReflectionTestUtils.setField(venta, "total", new BigDecimal(total));
        ReflectionTestUtils.setField(venta, "fecha", fecha);
        return venta;
    }

    private PagoCliente pago(MedioPago medioPago, String monto, Instant fecha) {
        PagoCliente pago = new PagoCliente(1L, new BigDecimal(monto), medioPago, 1L, 1L);
        ReflectionTestUtils.setField(pago, "fecha", fecha);
        return pago;
    }

    @Test
    void ingresosPorMedioPagoExcluyeVentasACuentaCorrienteYSumaLoDemas() {
        Instant hoy = Instant.parse("2026-09-05T15:00:00Z");
        when(ventaService.listarEntrePeriodo(any(), any())).thenReturn(List.of(
                venta(MedioPago.EFECTIVO, "100.00", hoy),
                venta(MedioPago.CUENTA_CORRIENTE, "50.00", hoy)));
        when(cuentaCorrienteService.listarPagosEntrePeriodo(any(), any())).thenReturn(List.of());

        ReporteIngresosResponse reporte = reportesService.ingresosPorMedioPago(
                LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 5));

        assertThat(reporte.totalesPorMedioPago()).containsOnly(entry(MedioPago.EFECTIVO, "100.00"));
        assertThat(reporte.porDia()).hasSize(1);
        assertThat(reporte.porDia().get(0).total()).isEqualByComparingTo("100.00");
    }

    @Test
    void ingresosPorMedioPagoSumaCobrosDeCuentaCorrienteJuntoConLasVentas() {
        Instant hoy = Instant.parse("2026-09-05T15:00:00Z");
        when(ventaService.listarEntrePeriodo(any(), any())).thenReturn(List.of(
                venta(MedioPago.EFECTIVO, "100.00", hoy)));
        when(cuentaCorrienteService.listarPagosEntrePeriodo(any(), any())).thenReturn(List.of(
                pago(MedioPago.EFECTIVO, "30.00", hoy),
                pago(MedioPago.TRANSFERENCIA, "20.00", hoy)));

        ReporteIngresosResponse reporte = reportesService.ingresosPorMedioPago(
                LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 5));

        assertThat(reporte.totalesPorMedioPago())
                .containsOnly(entry(MedioPago.EFECTIVO, "130.00"), entry(MedioPago.TRANSFERENCIA, "20.00"));
    }

    private static org.assertj.core.data.MapEntry<MedioPago, BigDecimal> entry(MedioPago medioPago, String monto) {
        return org.assertj.core.data.MapEntry.entry(medioPago, new BigDecimal(monto));
    }
}
