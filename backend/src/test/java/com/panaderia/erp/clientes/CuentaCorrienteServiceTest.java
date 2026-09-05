package com.panaderia.erp.clientes;

import com.panaderia.erp.caja.Caja;
import com.panaderia.erp.caja.CajaRepository;
import com.panaderia.erp.caja.EstadoCaja;
import com.panaderia.erp.clientes.dto.PagoClienteRequest;
import com.panaderia.erp.clientes.dto.SaldoClienteResponse;
import com.panaderia.erp.core.auditoria.AuditoriaService;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.usuario.Rol;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import com.panaderia.erp.ventas.MedioPago;
import com.panaderia.erp.ventas.VentaService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaCorrienteServiceTest {

    @Mock
    private ClienteService clienteService;

    @Mock
    private PagoClienteRepository pagoClienteRepository;

    @Mock
    private VentaService ventaService;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private CajaRepository cajaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CuentaCorrienteService cuentaCorrienteService;

    private Cliente cliente(long id, String nombre) {
        Cliente cliente = new Cliente(nombre, "1234", true);
        ReflectionTestUtils.setField(cliente, "id", id);
        return cliente;
    }

    @Test
    void elSaldoEsElTotalVendidoACuentaCorrienteMenosLosPagos() {
        Cliente cliente = cliente(1L, "Juan Pérez");
        when(clienteService.obtenerPorId(1L)).thenReturn(cliente);
        when(ventaService.totalVendidoACuentaCorriente(1L)).thenReturn(new BigDecimal("5000.00"));
        when(pagoClienteRepository.sumMontoPorCliente(1L)).thenReturn(new BigDecimal("2000.00"));

        SaldoClienteResponse saldo = cuentaCorrienteService.consultarSaldo(1L);

        assertThat(saldo.clienteId()).isEqualTo(1L);
        assertThat(saldo.totalVentasCuentaCorriente()).isEqualByComparingTo("5000.00");
        assertThat(saldo.totalPagos()).isEqualByComparingTo("2000.00");
        assertThat(saldo.saldo()).isEqualByComparingTo("3000.00");
    }

    @Test
    void unClienteSinVentasNiPagosTieneSaldoCero() {
        Cliente cliente = cliente(2L, "Sin Movimientos");
        when(clienteService.obtenerPorId(2L)).thenReturn(cliente);
        when(ventaService.totalVendidoACuentaCorriente(2L)).thenReturn(BigDecimal.ZERO);
        when(pagoClienteRepository.sumMontoPorCliente(2L)).thenReturn(BigDecimal.ZERO);

        SaldoClienteResponse saldo = cuentaCorrienteService.consultarSaldo(2L);

        assertThat(saldo.saldo()).isEqualByComparingTo("0");
    }

    @Test
    void siElClienteNoExisteNoSeConsultaNadaDeVentasNiPagos() {
        when(clienteService.obtenerPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("Cliente no encontrado: 99"));

        assertThatThrownBy(() -> cuentaCorrienteService.consultarSaldo(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(ventaService, never()).totalVendidoACuentaCorriente(any());
        verify(pagoClienteRepository, never()).sumMontoPorCliente(any());
    }

    @Test
    void registrarPagoLoGuardaConElClienteYMedioIndicados() {
        Cliente cliente = cliente(1L, "Juan Pérez");
        Usuario vendedor = new Usuario("Vendedora", "vendedor@panaderia.local", "hash", Rol.VENDEDOR);
        ReflectionTestUtils.setField(vendedor, "id", 7L);

        when(clienteService.obtenerPorId(1L)).thenReturn(cliente);
        when(usuarioRepository.findByEmail("vendedor@panaderia.local")).thenReturn(Optional.of(vendedor));
        when(cajaRepository.findFirstByEstado(EstadoCaja.ABIERTA)).thenReturn(Optional.empty());
        when(pagoClienteRepository.save(any(PagoCliente.class))).thenAnswer(inv -> inv.getArgument(0));

        PagoClienteRequest request = new PagoClienteRequest(new BigDecimal("1500.00"), MedioPago.EFECTIVO);
        PagoCliente pago = cuentaCorrienteService.registrarPago(1L, request, "vendedor@panaderia.local");

        assertThat(pago.getClienteId()).isEqualTo(1L);
        assertThat(pago.getMonto()).isEqualByComparingTo("1500.00");
        assertThat(pago.getMedioPago()).isEqualTo(MedioPago.EFECTIVO);
        assertThat(pago.getUsuarioId()).isEqualTo(7L);
        assertThat(pago.getCajaId()).isNull();
    }

    @Test
    void registrarPagoConCajaAbiertaLoAtaAEseTurno() {
        Cliente cliente = cliente(1L, "Juan Pérez");
        Usuario vendedor = new Usuario("Vendedora", "vendedor@panaderia.local", "hash", Rol.VENDEDOR);
        ReflectionTestUtils.setField(vendedor, "id", 7L);
        Caja caja = new Caja(new BigDecimal("1000.00"), 7L);
        ReflectionTestUtils.setField(caja, "id", 3L);

        when(clienteService.obtenerPorId(1L)).thenReturn(cliente);
        when(usuarioRepository.findByEmail("vendedor@panaderia.local")).thenReturn(Optional.of(vendedor));
        when(cajaRepository.findFirstByEstado(EstadoCaja.ABIERTA)).thenReturn(Optional.of(caja));
        when(pagoClienteRepository.save(any(PagoCliente.class))).thenAnswer(inv -> inv.getArgument(0));

        PagoClienteRequest request = new PagoClienteRequest(new BigDecimal("500.00"), MedioPago.EFECTIVO);
        PagoCliente pago = cuentaCorrienteService.registrarPago(1L, request, "vendedor@panaderia.local");

        assertThat(pago.getCajaId()).isEqualTo(3L);
        assertThat(pago.getUsuarioId()).isEqualTo(7L);
    }
}
