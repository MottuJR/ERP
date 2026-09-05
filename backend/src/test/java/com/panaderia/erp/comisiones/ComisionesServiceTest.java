package com.panaderia.erp.comisiones;

import com.panaderia.erp.clientes.CuentaCorrienteService;
import com.panaderia.erp.clientes.dto.PagoTurnoResumen;
import com.panaderia.erp.comisiones.dto.ComisionProduccionResponse;
import com.panaderia.erp.comisiones.dto.ComisionVendedorResponse;
import com.panaderia.erp.core.usuario.Rol;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import com.panaderia.erp.produccion.OrdenProduccion;
import com.panaderia.erp.produccion.OrdenProduccionService;
import com.panaderia.erp.productos.Categoria;
import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import com.panaderia.erp.productos.TipoProducto;
import com.panaderia.erp.productos.UnidadMedida;
import com.panaderia.erp.ventas.VentaService;
import com.panaderia.erp.ventas.dto.VentaTurnoResumen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComisionesServiceTest {

    @Mock
    private VentaService ventaService;

    @Mock
    private CuentaCorrienteService cuentaCorrienteService;

    @Mock
    private OrdenProduccionService ordenProduccionService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private ComisionesService comisionesService;

    private static final Instant DESDE = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-09-02T00:00:00Z");

    private Usuario usuarioConComision(long id, String nombre, String porcentaje) {
        Usuario usuario = new Usuario(nombre, nombre.toLowerCase() + "@panaderia.local", "hash", Rol.VENDEDOR);
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setPorcentajeComision(porcentaje == null ? null : new BigDecimal(porcentaje));
        return usuario;
    }

    @Test
    void comisionDeVendedorEsElTotalVendidoEnElTurnoPorSuPorcentaje() {
        Usuario vendedora = usuarioConComision(1L, "Vendedora", "5.00");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedora));
        when(ventaService.totalVendidoPorTurnoYUsuario(DESDE, HASTA))
                .thenReturn(List.of(new VentaTurnoResumen(10L, 1L, new BigDecimal("2000.00"))));
        when(cuentaCorrienteService.totalPagadoPorTurnoYUsuario(DESDE, HASTA)).thenReturn(List.of());

        List<ComisionVendedorResponse> comisiones = comisionesService.comisionesVendedores(DESDE, HASTA);

        assertThat(comisiones).hasSize(1);
        ComisionVendedorResponse comision = comisiones.get(0);
        assertThat(comision.cajaId()).isEqualTo(10L);
        assertThat(comision.usuarioId()).isEqualTo(1L);
        // 2000.00 * 5% = 100.00
        assertThat(comision.comision()).isEqualByComparingTo("100.00");
    }

    @Test
    void unVendedorSinPorcentajeAsignadoTieneComisionCero() {
        Usuario vendedor = usuarioConComision(2L, "SinComision", null);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(vendedor));
        when(ventaService.totalVendidoPorTurnoYUsuario(DESDE, HASTA))
                .thenReturn(List.of(new VentaTurnoResumen(11L, 2L, new BigDecimal("5000.00"))));
        when(cuentaCorrienteService.totalPagadoPorTurnoYUsuario(DESDE, HASTA)).thenReturn(List.of());

        List<ComisionVendedorResponse> comisiones = comisionesService.comisionesVendedores(DESDE, HASTA);

        assertThat(comisiones.get(0).comision()).isEqualByComparingTo("0");
        assertThat(comisiones.get(0).porcentaje()).isNull();
    }

    @Test
    void sumaPorSeparadoCadaTurnoDelMismoVendedor() {
        Usuario vendedora = usuarioConComision(1L, "Vendedora", "10.00");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedora));
        when(ventaService.totalVendidoPorTurnoYUsuario(DESDE, HASTA))
                .thenReturn(List.of(
                        new VentaTurnoResumen(10L, 1L, new BigDecimal("1000.00")),
                        new VentaTurnoResumen(20L, 1L, new BigDecimal("3000.00"))));
        when(cuentaCorrienteService.totalPagadoPorTurnoYUsuario(DESDE, HASTA)).thenReturn(List.of());

        List<ComisionVendedorResponse> comisiones = comisionesService.comisionesVendedores(DESDE, HASTA);

        assertThat(comisiones).hasSize(2);
        assertThat(comisiones.get(0).comision()).isEqualByComparingTo("100.00");
        assertThat(comisiones.get(1).comision()).isEqualByComparingTo("300.00");
    }

    @Test
    void unCobroDeCuentaCorrienteSumaALaComisionJuntoConLoVendido() {
        Usuario vendedora = usuarioConComision(1L, "Vendedora", "10.00");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedora));
        when(ventaService.totalVendidoPorTurnoYUsuario(DESDE, HASTA))
                .thenReturn(List.of(new VentaTurnoResumen(10L, 1L, new BigDecimal("1000.00"))));
        when(cuentaCorrienteService.totalPagadoPorTurnoYUsuario(DESDE, HASTA))
                .thenReturn(List.of(new PagoTurnoResumen(10L, 1L, new BigDecimal("500.00"))));

        List<ComisionVendedorResponse> comisiones = comisionesService.comisionesVendedores(DESDE, HASTA);

        assertThat(comisiones).hasSize(1);
        ComisionVendedorResponse comision = comisiones.get(0);
        assertThat(comision.totalVendido()).isEqualByComparingTo("1000.00");
        assertThat(comision.totalCobrado()).isEqualByComparingTo("500.00");
        // (1000.00 + 500.00) * 10% = 150.00
        assertThat(comision.comision()).isEqualByComparingTo("150.00");
    }

    @Test
    void unTurnoConSoloCobrosYSinVentasTambienGeneraComision() {
        Usuario vendedora = usuarioConComision(1L, "Vendedora", "10.00");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedora));
        when(ventaService.totalVendidoPorTurnoYUsuario(DESDE, HASTA)).thenReturn(List.of());
        when(cuentaCorrienteService.totalPagadoPorTurnoYUsuario(DESDE, HASTA))
                .thenReturn(List.of(new PagoTurnoResumen(10L, 1L, new BigDecimal("500.00"))));

        List<ComisionVendedorResponse> comisiones = comisionesService.comisionesVendedores(DESDE, HASTA);

        assertThat(comisiones).hasSize(1);
        assertThat(comisiones.get(0).totalVendido()).isEqualByComparingTo("0");
        assertThat(comisiones.get(0).totalCobrado()).isEqualByComparingTo("500.00");
        assertThat(comisiones.get(0).comision()).isEqualByComparingTo("50.00");
    }

    @Test
    void comisionDeProduccionEsCantidadPorPrecioPorPorcentaje() {
        Usuario empleado = usuarioConComision(3L, "Panadero", "2.50");
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(empleado));

        Producto pan = new Producto("Pan", new Categoria("Panificados"), TipoProducto.ELABORADO,
                false, new BigDecimal("1000.00"), UnidadMedida.UNIDAD, "7790000000001", null, BigDecimal.ZERO);
        ReflectionTestUtils.setField(pan, "id", 5L);
        when(productoService.obtenerPorId(5L)).thenReturn(pan);

        OrdenProduccion orden = new OrdenProduccion(5L, new BigDecimal("20"), 3L);
        ReflectionTestUtils.setField(orden, "id", 99L);
        when(ordenProduccionService.listarEntrePeriodo(DESDE, HASTA)).thenReturn(List.of(orden));

        List<ComisionProduccionResponse> comisiones = comisionesService.comisionesProduccion(DESDE, HASTA);

        assertThat(comisiones).hasSize(1);
        // 20 unidades * 1000.00 * 2.5% = 500.00
        assertThat(comisiones.get(0).comision()).isEqualByComparingTo("500.00");
        assertThat(comisiones.get(0).ordenId()).isEqualTo(99L);
    }
}
