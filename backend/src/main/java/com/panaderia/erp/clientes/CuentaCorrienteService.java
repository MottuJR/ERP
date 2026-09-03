package com.panaderia.erp.clientes;

import com.panaderia.erp.clientes.dto.PagoClienteRequest;
import com.panaderia.erp.clientes.dto.SaldoClienteResponse;
import com.panaderia.erp.core.auditoria.AccionAuditoria;
import com.panaderia.erp.core.auditoria.AuditoriaService;
import com.panaderia.erp.ventas.VentaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CuentaCorrienteService {

    private static final String ENTIDAD = "Cliente";

    private final ClienteService clienteService;
    private final PagoClienteRepository pagoClienteRepository;
    private final VentaService ventaService;
    private final AuditoriaService auditoriaService;

    public CuentaCorrienteService(ClienteService clienteService, PagoClienteRepository pagoClienteRepository,
                                   VentaService ventaService, AuditoriaService auditoriaService) {
        this.clienteService = clienteService;
        this.pagoClienteRepository = pagoClienteRepository;
        this.ventaService = ventaService;
        this.auditoriaService = auditoriaService;
    }

    public List<PagoCliente> listarPagos(Long clienteId) {
        clienteService.obtenerPorId(clienteId);
        return pagoClienteRepository.findByClienteIdOrderByFechaDesc(clienteId);
    }

    @Transactional
    public PagoCliente registrarPago(Long clienteId, PagoClienteRequest request, String emailUsuario) {
        Cliente cliente = clienteService.obtenerPorId(clienteId);
        PagoCliente pago =
                pagoClienteRepository.save(new PagoCliente(cliente.getId(), request.monto(), request.medioPago()));

        // Se audita explícitamente (además de quedar reflejado en el saldo de cuenta corriente)
        // porque ahora también puede registrarlo un VENDEDOR, no solo DUENO/ENCARGADO.
        auditoriaService.registrar(emailUsuario, ENTIDAD, cliente.getId(), AccionAuditoria.REGISTRAR_PAGO,
                "Pago de %s (%s) de \"%s\"".formatted(request.monto(), request.medioPago(), cliente.getNombre()));

        return pago;
    }

    public SaldoClienteResponse consultarSaldo(Long clienteId) {
        Cliente cliente = clienteService.obtenerPorId(clienteId);

        BigDecimal totalVentas = ventaService.totalVendidoACuentaCorriente(clienteId);
        BigDecimal totalPagos = pagoClienteRepository.sumMontoPorCliente(clienteId);
        BigDecimal saldo = totalVentas.subtract(totalPagos);

        return new SaldoClienteResponse(cliente.getId(), cliente.getNombre(), totalVentas, totalPagos, saldo);
    }
}
