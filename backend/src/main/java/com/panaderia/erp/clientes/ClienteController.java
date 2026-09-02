package com.panaderia.erp.clientes;

import com.panaderia.erp.clientes.dto.ClienteRequest;
import com.panaderia.erp.clientes.dto.ClienteResponse;
import com.panaderia.erp.clientes.dto.PagoClienteRequest;
import com.panaderia.erp.clientes.dto.PagoClienteResponse;
import com.panaderia.erp.clientes.dto.SaldoClienteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final CuentaCorrienteService cuentaCorrienteService;

    public ClienteController(ClienteService clienteService, CuentaCorrienteService cuentaCorrienteService) {
        this.clienteService = clienteService;
        this.cuentaCorrienteService = cuentaCorrienteService;
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return clienteService.listarActivos().stream()
                .map(ClienteResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable Long id) {
        return ClienteResponse.from(clienteService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        Cliente cliente = clienteService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteResponse.from(cliente));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
    public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return ClienteResponse.from(clienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        clienteService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/saldo")
    public SaldoClienteResponse saldo(@PathVariable Long id) {
        return cuentaCorrienteService.consultarSaldo(id);
    }

    @GetMapping("/{id}/pagos")
    public List<PagoClienteResponse> listarPagos(@PathVariable Long id) {
        return cuentaCorrienteService.listarPagos(id).stream()
                .map(PagoClienteResponse::from)
                .toList();
    }

    @PostMapping("/{id}/pagos")
    @PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
    public ResponseEntity<PagoClienteResponse> registrarPago(@PathVariable Long id,
                                                              @Valid @RequestBody PagoClienteRequest request) {
        PagoCliente pago = cuentaCorrienteService.registrarPago(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PagoClienteResponse.from(pago));
    }
}
