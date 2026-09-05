package com.panaderia.erp.clientes;

import com.panaderia.erp.caja.Caja;
import com.panaderia.erp.caja.CajaRepository;
import com.panaderia.erp.caja.EstadoCaja;
import com.panaderia.erp.clientes.dto.PagoClienteRequest;
import com.panaderia.erp.clientes.dto.PagoPorMedioPagoResumen;
import com.panaderia.erp.clientes.dto.PagoTurnoResumen;
import com.panaderia.erp.clientes.dto.SaldoClienteResponse;
import com.panaderia.erp.clientes.dto.VentaClienteResponse;
import com.panaderia.erp.core.auditoria.AccionAuditoria;
import com.panaderia.erp.core.auditoria.AuditoriaService;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import com.panaderia.erp.ventas.Venta;
import com.panaderia.erp.ventas.VentaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class CuentaCorrienteService {

    private static final String ENTIDAD = "Cliente";

    private final ClienteService clienteService;
    private final PagoClienteRepository pagoClienteRepository;
    private final VentaService ventaService;
    private final AuditoriaService auditoriaService;
    private final CajaRepository cajaRepository;
    private final UsuarioRepository usuarioRepository;

    public CuentaCorrienteService(ClienteService clienteService, PagoClienteRepository pagoClienteRepository,
                                   VentaService ventaService, AuditoriaService auditoriaService,
                                   CajaRepository cajaRepository, UsuarioRepository usuarioRepository) {
        this.clienteService = clienteService;
        this.pagoClienteRepository = pagoClienteRepository;
        this.ventaService = ventaService;
        this.auditoriaService = auditoriaService;
        this.cajaRepository = cajaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<PagoCliente> listarPagos(Long clienteId) {
        clienteService.obtenerPorId(clienteId);
        return pagoClienteRepository.findByClienteIdOrderByFechaDesc(clienteId);
    }

    /**
     * Cobra la cuenta corriente igual que se registra una venta: si hay una caja abierta, el
     * cobro queda atado a ese turno y a quien lo cobró, para que entre en la contabilidad de
     * efectivo del turno y en la comisión del empleado (ver CajaService y ComisionesService).
     * Si no hay caja abierta el cobro se guarda igual, pero sin turno asignado.
     */
    @Transactional
    public PagoCliente registrarPago(Long clienteId, PagoClienteRequest request, String emailUsuario) {
        Cliente cliente = clienteService.obtenerPorId(clienteId);
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado"));
        Long cajaId = cajaRepository.findFirstByEstado(EstadoCaja.ABIERTA).map(Caja::getId).orElse(null);

        PagoCliente pago = pagoClienteRepository.save(
                new PagoCliente(cliente.getId(), request.monto(), request.medioPago(), cajaId, usuario.getId()));

        // Se audita explícitamente (además de quedar reflejado en el saldo de cuenta corriente)
        // porque ahora también puede registrarlo un VENDEDOR, no solo DUENO/ENCARGADO.
        auditoriaService.registrar(emailUsuario, ENTIDAD, cliente.getId(), AccionAuditoria.REGISTRAR_PAGO,
                "Pago de %s (%s) de \"%s\"".formatted(request.monto(), request.medioPago(), cliente.getNombre()));

        return pago;
    }

    /**
     * Cobros de cuenta corriente de un turno puntual, de más reciente a más viejo. La usa el
     * módulo de caja para el resumen de un turno cerrado.
     */
    public List<PagoCliente> listarPorCaja(Long cajaId) {
        return pagoClienteRepository.findByCajaIdOrderByFechaDesc(cajaId);
    }

    /**
     * Total cobrado y cantidad de cobros por medio de pago dentro de una caja puntual. La usa el
     * módulo de caja para el resumen de un turno.
     */
    public List<PagoPorMedioPagoResumen> resumenPorMedioPago(Long cajaId) {
        return pagoClienteRepository.totalPorMedioPago(cajaId).stream()
                .map(p -> new PagoPorMedioPagoResumen(p.getMedioPago(), p.getTotal(), p.getCantidad()))
                .toList();
    }

    /**
     * Total cobrado por cada (turno, empleado) en un período — solo cobros con caja asignada.
     * La usa el módulo de comisiones para sumarlo a lo vendido de cada empleado.
     */
    public List<PagoTurnoResumen> totalPagadoPorTurnoYUsuario(Instant desde, Instant hasta) {
        return pagoClienteRepository.totalPagadoPorTurnoYUsuario(desde, hasta).stream()
                .map(p -> new PagoTurnoResumen(p.getCajaId(), p.getUsuarioId(), p.getTotalPagado()))
                .toList();
    }

    /**
     * Compras (ventas) de un cliente en un período, de la más reciente a la más vieja.
     */
    public List<VentaClienteResponse> listarVentas(Long clienteId, Instant desde, Instant hasta) {
        clienteService.obtenerPorId(clienteId);
        return ventaService.listarPorClienteEntrePeriodo(clienteId, desde, hasta).stream()
                .map(this::aVentaClienteResponse)
                .toList();
    }

    private VentaClienteResponse aVentaClienteResponse(Venta venta) {
        return new VentaClienteResponse(venta.getId(), venta.getFecha(), venta.getTotal(), venta.getMedioPago());
    }

    public SaldoClienteResponse consultarSaldo(Long clienteId) {
        Cliente cliente = clienteService.obtenerPorId(clienteId);

        BigDecimal totalVentas = ventaService.totalVendidoACuentaCorriente(clienteId);
        BigDecimal totalPagos = pagoClienteRepository.sumMontoPorCliente(clienteId);
        BigDecimal saldo = totalVentas.subtract(totalPagos);

        return new SaldoClienteResponse(cliente.getId(), cliente.getNombre(), totalVentas, totalPagos, saldo);
    }
}
