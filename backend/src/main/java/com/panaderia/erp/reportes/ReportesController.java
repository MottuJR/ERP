package com.panaderia.erp.reportes;

import com.panaderia.erp.reportes.dto.MargenProductoResponse;
import com.panaderia.erp.reportes.dto.ProductoMasVendidoResponse;
import com.panaderia.erp.reportes.dto.ReporteVentasResponse;
import com.panaderia.erp.reportes.dto.StockCriticoResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasAnyRole('DUENO', 'ENCARGADO')")
public class ReportesController {

    private final ReportesService reportesService;

    public ReportesController(ReportesService reportesService) {
        this.reportesService = reportesService;
    }

    @GetMapping("/ventas")
    public ReporteVentasResponse ventas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportesService.reporteVentas(desde, hasta);
    }

    @GetMapping("/productos-mas-vendidos")
    public List<ProductoMasVendidoResponse> productosMasVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return reportesService.productosMasVendidos(desde, hasta, limite);
    }

    @GetMapping("/margen-productos")
    public List<MargenProductoResponse> margenProductos() {
        return reportesService.margenPorProducto();
    }

    @GetMapping("/stock-critico")
    public StockCriticoResponse stockCritico() {
        return reportesService.stockCritico();
    }
}
