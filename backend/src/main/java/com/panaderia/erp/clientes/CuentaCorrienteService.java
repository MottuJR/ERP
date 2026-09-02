package com.panaderia.erp.clientes;

import com.panaderia.erp.clientes.dto.PagoClienteRequest;
import com.panaderia.erp.clientes.dto.SaldoClienteResponse;
import com.panaderia.erp.ventas.VentaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CuentaCorrienteService {

    private final ClienteService clienteService;
    private final PagoClienteRepository pagoClienteRepository;
    private final VentaService ventaService;

    public CuentaCorrienteService(ClienteService clienteService, PagoClienteRepository pagoClienteRepository,
                                   VentaService ventaService) {
        this.clienteService = clienteService;
        this.pagoClienteRepository = pagoClienteRepository;
        this.ventaService = ventaService;
    }

    public List<PagoCliente> listarPagos(Long clienteId) {
        clienteService.obtenerPorId(clienteId);
        return pagoClienteRepository.findByClienteIdOrderByFechaDesc(clienteId);
    }

    @Transactional
    public PagoCliente registrarPago(Long clienteId, PagoClienteRequest request) {
        Cliente cliente = clienteService.obtenerPorId(clienteId);
        return pagoClienteRepository.save(new PagoCliente(cliente.getId(), request.monto(), request.medioPago()));
    }

    public SaldoClienteResponse consultarSaldo(Long clienteId) {
        Cliente cliente = clienteService.obtenerPorId(clienteId);

        BigDecimal totalVentas = ventaService.totalVendidoACuentaCorriente(clienteId);
        BigDecimal totalPagos = pagoClienteRepository.sumMontoPorCliente(clienteId);
        BigDecimal saldo = totalVentas.subtract(totalPagos);

        return new SaldoClienteResponse(cliente.getId(), cliente.getNombre(), totalVentas, totalPagos, saldo);
    }
}
