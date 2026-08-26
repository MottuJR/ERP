package com.panaderia.erp.compras;

import com.panaderia.erp.compras.dto.CompraResponse;
import com.panaderia.erp.compras.dto.ConfirmarCompraRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compras")
@PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @GetMapping("/{id}")
    public CompraResponse obtener(@PathVariable Long id) {
        return compraService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<CompraResponse> confirmar(@Valid @RequestBody ConfirmarCompraRequest request) {
        CompraResponse compra = compraService.confirmarCompra(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(compra);
    }
}
