package com.panaderia.erp.compras;

import com.panaderia.erp.compras.dto.ProveedorRequest;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public List<Proveedor> listarActivos() {
        return proveedorRepository.findByActivoTrue();
    }

    public Proveedor obtenerPorId(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + id));
    }

    @Transactional
    public Proveedor crear(ProveedorRequest request) {
        return proveedorRepository.save(
                new Proveedor(request.nombre(), request.contacto(), request.telefono(), request.email()));
    }

    @Transactional
    public Proveedor actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = obtenerPorId(id);
        proveedor.setNombre(request.nombre());
        proveedor.setContacto(request.contacto());
        proveedor.setTelefono(request.telefono());
        proveedor.setEmail(request.email());
        return proveedor;
    }

    @Transactional
    public void desactivar(Long id) {
        obtenerPorId(id).setActivo(false);
    }
}
