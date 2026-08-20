package com.panaderia.erp.ventas;

import com.panaderia.erp.ventas.dto.ConfirmarVentaRequest;
import com.panaderia.erp.ventas.dto.EscaneoResponse;
import com.panaderia.erp.ventas.dto.VentaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/ventas")
@PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO', 'VENDEDOR')")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping("/escanear")
    public EscaneoResponse escanear(@RequestParam String codigo,
                                     @RequestParam(required = false) BigDecimal cantidad) {
        return ventaService.escanear(codigo, cantidad);
    }

    @GetMapping("/{id}")
    public VentaResponse obtener(@PathVariable Long id) {
        return ventaService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<VentaResponse> confirmar(@Valid @RequestBody ConfirmarVentaRequest request,
                                                     Authentication authentication) {
        VentaResponse venta = ventaService.confirmarVenta(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(venta);
    }
}
