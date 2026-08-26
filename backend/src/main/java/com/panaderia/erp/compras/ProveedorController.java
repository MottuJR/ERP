package com.panaderia.erp.compras;

import com.panaderia.erp.compras.dto.ProveedorRequest;
import com.panaderia.erp.compras.dto.ProveedorResponse;
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
@RequestMapping("/api/proveedores")
@PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public List<ProveedorResponse> listar() {
        return proveedorService.listarActivos().stream()
                .map(ProveedorResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProveedorResponse obtener(@PathVariable Long id) {
        return ProveedorResponse.from(proveedorService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest request) {
        Proveedor proveedor = proveedorService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProveedorResponse.from(proveedor));
    }

    @PutMapping("/{id}")
    public ProveedorResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorRequest request) {
        return ProveedorResponse.from(proveedorService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        proveedorService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
