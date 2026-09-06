package com.panaderia.erp.reportes;

import com.panaderia.erp.clientes.CuentaCorrienteService;
import com.panaderia.erp.clientes.PagoCliente;
import com.panaderia.erp.core.util.RangoFechas;
import com.panaderia.erp.inventario.InventarioService;
import com.panaderia.erp.produccion.Receta;
import com.panaderia.erp.produccion.RecetaService;
import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import com.panaderia.erp.productos.TipoProducto;
import com.panaderia.erp.reportes.dto.IngresoDiaResponse;
import com.panaderia.erp.reportes.dto.MargenProductoResponse;
import com.panaderia.erp.reportes.dto.ProductoMasVendidoResponse;
import com.panaderia.erp.reportes.dto.ReporteIngresosResponse;
import com.panaderia.erp.reportes.dto.ReporteVentasResponse;
import com.panaderia.erp.reportes.dto.StockCriticoItemResponse;
import com.panaderia.erp.reportes.dto.StockCriticoResponse;
import com.panaderia.erp.reportes.dto.VentaDiaResponse;
import com.panaderia.erp.ventas.MedioPago;
import com.panaderia.erp.ventas.Venta;
import com.panaderia.erp.ventas.VentaService;
import com.panaderia.erp.ventas.dto.ProductoVendidoResumen;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Solo lee de otros módulos (Venta, Producto, Receta/Insumo) para armar reportes — no modifica
 * nada, como dice la sección 3 del documento de diseño.
 */
@Service
public class ReportesService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final InventarioService inventarioService;
    private final RecetaService recetaService;
    private final CuentaCorrienteService cuentaCorrienteService;

    public ReportesService(VentaService ventaService, ProductoService productoService,
                            InventarioService inventarioService, RecetaService recetaService,
                            CuentaCorrienteService cuentaCorrienteService) {
        this.ventaService = ventaService;
        this.productoService = productoService;
        this.inventarioService = inventarioService;
        this.recetaService = recetaService;
        this.cuentaCorrienteService = cuentaCorrienteService;
    }

    public ReporteVentasResponse reporteVentas(LocalDate desde, LocalDate hasta) {
        List<Venta> ventas = ventaService.listarEntrePeriodo(RangoFechas.inicioDelDia(desde), RangoFechas.finDelDia(hasta));

        BigDecimal totalVendido = ventas.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        long cantidadVentas = ventas.size();
        BigDecimal promedio = cantidadVentas == 0
                ? BigDecimal.ZERO
                : totalVendido.divide(BigDecimal.valueOf(cantidadVentas), 2, RoundingMode.HALF_UP);

        List<VentaDiaResponse> porDia = agruparPorDia(ventas);

        return new ReporteVentasResponse(desde, hasta, cantidadVentas, totalVendido, promedio, porDia);
    }

    /**
     * Ingresos reales por día y medio de pago: ventas (menos las hechas a cuenta corriente, que
     * todavía no son plata cobrada) más los cobros de cuenta corriente, que sí lo son y traen su
     * propio medio de pago. Sirve para comparar cuánto entra en efectivo vs. transferencia/tarjeta
     * a lo largo del tiempo.
     */
    public ReporteIngresosResponse ingresosPorMedioPago(LocalDate desde, LocalDate hasta) {
        Instant inicio = RangoFechas.inicioDelDia(desde);
        Instant fin = RangoFechas.finDelDia(hasta);
        ZoneId zona = ZoneId.systemDefault();

        Map<LocalDate, Map<MedioPago, BigDecimal>> porDia = new TreeMap<>();
        Map<MedioPago, BigDecimal> totales = new EnumMap<>(MedioPago.class);

        for (Venta venta : ventaService.listarEntrePeriodo(inicio, fin)) {
            if (venta.getMedioPago() != MedioPago.CUENTA_CORRIENTE) {
                acumular(porDia, totales, venta.getFecha().atZone(zona).toLocalDate(), venta.getMedioPago(), venta.getTotal());
            }
        }
        for (PagoCliente pago : cuentaCorrienteService.listarPagosEntrePeriodo(inicio, fin)) {
            acumular(porDia, totales, pago.getFecha().atZone(zona).toLocalDate(), pago.getMedioPago(), pago.getMonto());
        }

        List<IngresoDiaResponse> dias = porDia.entrySet().stream()
                .map(entry -> new IngresoDiaResponse(entry.getKey(), entry.getValue(),
                        entry.getValue().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)))
                .toList();

        return new ReporteIngresosResponse(desde, hasta, dias, totales);
    }

    private void acumular(Map<LocalDate, Map<MedioPago, BigDecimal>> porDia, Map<MedioPago, BigDecimal> totales,
                           LocalDate fecha, MedioPago medioPago, BigDecimal monto) {
        porDia.computeIfAbsent(fecha, f -> new EnumMap<>(MedioPago.class)).merge(medioPago, monto, BigDecimal::add);
        totales.merge(medioPago, monto, BigDecimal::add);
    }

    public List<ProductoMasVendidoResponse> productosMasVendidos(LocalDate desde, LocalDate hasta, int limite) {
        List<ProductoVendidoResumen> resumenes = ventaService.productosMasVendidos(
                RangoFechas.inicioDelDia(desde), RangoFechas.finDelDia(hasta));

        return resumenes.stream()
                .limit(limite)
                .map(r -> {
                    Producto producto = productoService.obtenerPorId(r.productoId());
                    return new ProductoMasVendidoResponse(
                            producto.getId(), producto.getNombre(), r.cantidadVendida(), r.montoTotal());
                })
                .toList();
    }

    public List<MargenProductoResponse> margenPorProducto() {
        return productoService.listarActivos().stream()
                .filter(p -> p.getTipo() == TipoProducto.ELABORADO)
                .map(this::calcularMargen)
                .filter(Objects::nonNull)
                .toList();
    }

    public StockCriticoResponse stockCritico() {
        List<StockCriticoItemResponse> productos = productoService.listarConStockCritico().stream()
                .map(p -> new StockCriticoItemResponse(p.getId(), p.getNombre(), p.getStockActual(), p.getStockMinimo()))
                .toList();

        List<StockCriticoItemResponse> insumos = inventarioService.listarConStockCritico().stream()
                .map(i -> new StockCriticoItemResponse(i.getId(), i.getNombre(), i.getStockActual(), i.getStockMinimo()))
                .toList();

        return new StockCriticoResponse(productos, insumos);
    }

    private MargenProductoResponse calcularMargen(Producto producto) {
        return recetaService.buscarPorProducto(producto.getId())
                .map((Receta receta) -> {
                    // costoInsumos es el costo de la tanda completa: hay que llevarlo a costo por
                    // unidad de producto dividiendo por cuánto rinde esa tanda.
                    BigDecimal costoInsumos = recetaService.costoInsumos(receta)
                            .divide(receta.getRendimiento(), 4, RoundingMode.HALF_UP);
                    BigDecimal margen = producto.getPrecioVenta().subtract(costoInsumos);
                    BigDecimal margenPorcentual = producto.getPrecioVenta().compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : margen.multiply(CIEN).divide(producto.getPrecioVenta(), 2, RoundingMode.HALF_UP);

                    return new MargenProductoResponse(
                            producto.getId(), producto.getNombre(), producto.getPrecioVenta(),
                            costoInsumos, margen, margenPorcentual);
                })
                .orElse(null);
    }

    private List<VentaDiaResponse> agruparPorDia(List<Venta> ventas) {
        ZoneId zona = ZoneId.systemDefault();

        Map<LocalDate, List<Venta>> porDia = ventas.stream()
                .collect(Collectors.groupingBy(v -> v.getFecha().atZone(zona).toLocalDate(), TreeMap::new, Collectors.toList()));

        return porDia.entrySet().stream()
                .map(entry -> {
                    BigDecimal total = entry.getValue().stream()
                            .map(Venta::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new VentaDiaResponse(entry.getKey(), entry.getValue().size(), total);
                })
                .sorted(Comparator.comparing(VentaDiaResponse::fecha))
                .toList();
    }
}
