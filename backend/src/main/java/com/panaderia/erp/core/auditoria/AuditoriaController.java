package com.panaderia.erp.core.auditoria;

import com.panaderia.erp.core.auditoria.dto.RegistroAuditoriaResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('DUENO')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<RegistroAuditoriaResponse> listar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId) {
        return auditoriaService.listar(entidad, entidadId);
    }
}
