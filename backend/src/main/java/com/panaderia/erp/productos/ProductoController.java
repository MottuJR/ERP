package com.panaderia.erp.productos;

import com.panaderia.erp.productos.dto.ActualizarProductoRequest;
import com.panaderia.erp.productos.dto.CrearProductoRequest;
import com.panaderia.erp.productos.dto.ProductoResponse;
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
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoResponse> listar() {
        return productoService.listarActivos().stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return ProductoResponse.from(productoService.obtenerPorId(id));
    }

    @GetMapping("/codigo/{codigo}")
    public ProductoResponse buscarPorCodigo(@PathVariable String codigo) {
        return ProductoResponse.from(productoService.obtenerPorCodigoBarras(codigo));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody CrearProductoRequest request) {
        Producto producto = productoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductoResponse.from(producto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarProductoRequest request) {
        return ProductoResponse.from(productoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
