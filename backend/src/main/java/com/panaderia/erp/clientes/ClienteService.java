package com.panaderia.erp.clientes;

import com.panaderia.erp.clientes.dto.ClienteRequest;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listarActivos() {
        return clienteRepository.findByActivoTrue();
    }

    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));
    }

    /**
     * Valida que el cliente exista y tenga cuenta corriente habilitada. La usa el módulo de
     * ventas al confirmar una venta con medioPago = CUENTA_CORRIENTE.
     */
    public Cliente validarTieneCuentaCorriente(Long clienteId) {
        Cliente cliente = obtenerPorId(clienteId);

        if (!cliente.isTieneCuentaCorriente()) {
            throw new ValidacionNegocioException(
                    "El cliente \"" + cliente.getNombre() + "\" no tiene cuenta corriente habilitada");
        }

        return cliente;
    }

    @Transactional
    public Cliente crear(ClienteRequest request) {
        return clienteRepository.save(
                new Cliente(request.nombre(), request.telefono(), request.tieneCuentaCorriente()));
    }

    @Transactional
    public Cliente actualizar(Long id, ClienteRequest request) {
        Cliente cliente = obtenerPorId(id);
        cliente.setNombre(request.nombre());
        cliente.setTelefono(request.telefono());
        cliente.setTieneCuentaCorriente(request.tieneCuentaCorriente());
        return cliente;
    }

    @Transactional
    public void desactivar(Long id) {
        obtenerPorId(id).setActivo(false);
    }
}
