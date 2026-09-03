package com.panaderia.erp.caja;

import com.panaderia.erp.caja.dto.AbrirCajaRequest;
import com.panaderia.erp.caja.dto.CajaHistorialResponse;
import com.panaderia.erp.caja.dto.CajaResponse;
import com.panaderia.erp.caja.dto.CajaResumenResponse;
import com.panaderia.erp.caja.dto.CerrarCajaRequest;
import com.panaderia.erp.caja.dto.MovimientoCajaRequest;
import com.panaderia.erp.caja.dto.MovimientoCajaResponse;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/caja")
@PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO', 'VENDEDOR')")
public class CajaController {

    private final CajaService cajaService;

    public CajaController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    @GetMapping("/actual")
    public CajaResponse actual() {
        return cajaService.obtenerCajaAbierta()
                .map(CajaResponse::from)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay ninguna caja abierta"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
    public List<CajaHistorialResponse> historial() {
        return cajaService.listarHistorial();
    }

    @GetMapping("/{id}/resumen")
    @PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
    public CajaResumenResponse resumen(@PathVariable Long id) {
        return cajaService.obtenerResumen(id);
    }

    @GetMapping("/{id}")
    public CajaResponse obtener(@PathVariable Long id) {
        return CajaResponse.from(cajaService.obtenerPorId(id));
    }

    @PostMapping("/abrir")
    public ResponseEntity<CajaResponse> abrir(@Valid @RequestBody AbrirCajaRequest request,
                                               Authentication authentication) {
        Caja caja = cajaService.abrirTurno(request.montoInicial(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(CajaResponse.from(caja));
    }

    @PostMapping("/{id}/cerrar")
    public CajaResponse cerrar(@PathVariable Long id, @Valid @RequestBody CerrarCajaRequest request,
                                Authentication authentication) {
        return CajaResponse.from(cajaService.cerrarTurno(id, request.montoFinal(), authentication.getName()));
    }

    @GetMapping("/{id}/movimientos")
    public List<MovimientoCajaResponse> listarMovimientos(@PathVariable Long id) {
        return cajaService.listarMovimientos(id).stream()
                .map(MovimientoCajaResponse::from)
                .toList();
    }

    @PostMapping("/{id}/movimientos")
    public ResponseEntity<MovimientoCajaResponse> registrarMovimiento(
            @PathVariable Long id, @Valid @RequestBody MovimientoCajaRequest request) {
        MovimientoCaja movimiento =
                cajaService.registrarMovimiento(id, request.tipo(), request.monto(), request.concepto());
        return ResponseEntity.status(HttpStatus.CREATED).body(MovimientoCajaResponse.from(movimiento));
    }
}
