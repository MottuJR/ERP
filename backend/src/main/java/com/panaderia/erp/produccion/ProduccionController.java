package com.panaderia.erp.produccion;

import com.panaderia.erp.produccion.dto.ActualizarRecetaRequest;
import com.panaderia.erp.produccion.dto.CrearOrdenProduccionRequest;
import com.panaderia.erp.produccion.dto.CrearRecetaRequest;
import com.panaderia.erp.produccion.dto.OrdenProduccionResponse;
import com.panaderia.erp.produccion.dto.RecetaResponse;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produccion")
@PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
public class ProduccionController {

    private final RecetaService recetaService;
    private final OrdenProduccionService ordenProduccionService;

    public ProduccionController(RecetaService recetaService, OrdenProduccionService ordenProduccionService) {
        this.recetaService = recetaService;
        this.ordenProduccionService = ordenProduccionService;
    }

    @GetMapping("/recetas/{productoId}")
    public RecetaResponse obtenerReceta(@PathVariable Long productoId) {
        return recetaService.aResponse(recetaService.obtenerPorProducto(productoId));
    }

    @PostMapping("/recetas")
    public ResponseEntity<RecetaResponse> crearReceta(@Valid @RequestBody CrearRecetaRequest request) {
        Receta receta = recetaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(recetaService.aResponse(receta));
    }

    @PutMapping("/recetas/{productoId}")
    public RecetaResponse actualizarReceta(@PathVariable Long productoId,
                                            @Valid @RequestBody ActualizarRecetaRequest request) {
        return recetaService.aResponse(recetaService.actualizar(productoId, request));
    }

    @GetMapping("/ordenes/{id}")
    public OrdenProduccionResponse obtenerOrden(@PathVariable Long id) {
        return ordenProduccionService.obtenerPorId(id);
    }

    @PostMapping("/ordenes")
    public ResponseEntity<OrdenProduccionResponse> confirmarOrden(
            @Valid @RequestBody CrearOrdenProduccionRequest request, Authentication authentication) {
        OrdenProduccionResponse orden = ordenProduccionService.confirmar(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(orden);
    }
}
