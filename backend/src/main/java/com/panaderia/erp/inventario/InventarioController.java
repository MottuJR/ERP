package com.panaderia.erp.inventario;

import com.panaderia.erp.inventario.dto.InsumoRequest;
import com.panaderia.erp.inventario.dto.InsumoResponse;
import com.panaderia.erp.inventario.dto.MovimientoManualRequest;
import com.panaderia.erp.inventario.dto.MovimientoStockResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/insumos")
    public List<InsumoResponse> listarInsumos() {
        return inventarioService.listarInsumos().stream()
                .map(InsumoResponse::from)
                .toList();
    }

    @GetMapping("/insumos/{id}")
    public InsumoResponse obtenerInsumo(@PathVariable Long id) {
        return InsumoResponse.from(inventarioService.obtenerInsumoPorId(id));
    }

    @PostMapping("/insumos")
    @PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
    public ResponseEntity<InsumoResponse> crearInsumo(@Valid @RequestBody InsumoRequest request) {
        Insumo insumo = inventarioService.crearInsumo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(InsumoResponse.from(insumo));
    }

    @PutMapping("/insumos/{id}")
    @PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
    public InsumoResponse actualizarInsumo(@PathVariable Long id, @Valid @RequestBody InsumoRequest request) {
        return InsumoResponse.from(inventarioService.actualizarInsumo(id, request));
    }

    @GetMapping("/movimientos")
    public List<MovimientoStockResponse> listarMovimientos(
            @RequestParam ItemTipo itemTipo, @RequestParam Long itemId) {
        return inventarioService.listarMovimientos(itemTipo, itemId).stream()
                .map(MovimientoStockResponse::from)
                .toList();
    }

    @PostMapping("/movimientos")
    @PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
    public ResponseEntity<MovimientoStockResponse> registrarMovimiento(
            @Valid @RequestBody MovimientoManualRequest request, Authentication authentication) {
        MovimientoStock movimiento = inventarioService.registrarMovimientoManual(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(MovimientoStockResponse.from(movimiento));
    }
}
