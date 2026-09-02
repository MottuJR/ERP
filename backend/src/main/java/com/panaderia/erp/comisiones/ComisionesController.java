package com.panaderia.erp.comisiones;

import com.panaderia.erp.comisiones.dto.ComisionProduccionResponse;
import com.panaderia.erp.comisiones.dto.ComisionVendedorResponse;
import com.panaderia.erp.core.util.RangoFechas;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/comisiones")
@PreAuthorize("hasRole('DUENO')")
public class ComisionesController {

    private final ComisionesService comisionesService;

    public ComisionesController(ComisionesService comisionesService) {
        this.comisionesService = comisionesService;
    }

    @GetMapping("/vendedores")
    public List<ComisionVendedorResponse> comisionesVendedores(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return comisionesService.comisionesVendedores(RangoFechas.inicioDelDia(desde), RangoFechas.finDelDia(hasta));
    }

    @GetMapping("/produccion")
    public List<ComisionProduccionResponse> comisionesProduccion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return comisionesService.comisionesProduccion(RangoFechas.inicioDelDia(desde), RangoFechas.finDelDia(hasta));
    }
}
