package com.panaderia.erp.comisiones;

import com.panaderia.erp.clientes.CuentaCorrienteService;
import com.panaderia.erp.clientes.dto.PagoTurnoResumen;
import com.panaderia.erp.comisiones.dto.ComisionProduccionResponse;
import com.panaderia.erp.comisiones.dto.ComisionVendedorResponse;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import com.panaderia.erp.clientes.PagoCliente;
import com.panaderia.erp.produccion.OrdenProduccion;
import com.panaderia.erp.produccion.OrdenProduccionService;
import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import com.panaderia.erp.ventas.Venta;
import com.panaderia.erp.ventas.VentaService;
import com.panaderia.erp.ventas.dto.VentaTurnoResumen;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula comisiones on-demand (sección 7 del documento de diseño): no se persisten como
 * movimientos todavía, solo se leen Venta, OrdenProduccion, Caja y el porcentaje de cada
 * usuario para armar el reporte.
 */
@Service
public class ComisionesService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final VentaService ventaService;
    private final CuentaCorrienteService cuentaCorrienteService;
    private final OrdenProduccionService ordenProduccionService;
    private final UsuarioRepository usuarioRepository;
    private final ProductoService productoService;

    public ComisionesService(VentaService ventaService, CuentaCorrienteService cuentaCorrienteService,
                              OrdenProduccionService ordenProduccionService, UsuarioRepository usuarioRepository,
                              ProductoService productoService) {
        this.ventaService = ventaService;
        this.cuentaCorrienteService = cuentaCorrienteService;
        this.ordenProduccionService = ordenProduccionService;
        this.usuarioRepository = usuarioRepository;
        this.productoService = productoService;
    }

    /**
     * Por cada turno (Caja) y vendedor: comisión = (total vendido + total cobrado de cuenta
     * corriente) por ese vendedor en ese turno × su porcentaje de comisión. Cobrar una deuda de
     * cuenta corriente cuenta para la comisión igual que una venta — es la misma gestión con el
     * cliente en el mostrador.
     */
    public List<ComisionVendedorResponse> comisionesVendedores(Instant desde, Instant hasta) {
        record Turno(Long cajaId, Long usuarioId) {
        }

        Map<Turno, BigDecimal> totalVendido = new LinkedHashMap<>();
        Map<Turno, BigDecimal> totalCobrado = new LinkedHashMap<>();

        for (VentaTurnoResumen r : ventaService.totalVendidoPorTurnoYUsuario(desde, hasta)) {
            totalVendido.merge(new Turno(r.cajaId(), r.usuarioId()), r.totalVendido(), BigDecimal::add);
        }
        for (PagoTurnoResumen r : cuentaCorrienteService.totalPagadoPorTurnoYUsuario(desde, hasta)) {
            totalCobrado.merge(new Turno(r.cajaId(), r.usuarioId()), r.totalPagado(), BigDecimal::add);
        }

        Map<Turno, BigDecimal> turnos = new LinkedHashMap<>(totalVendido);
        totalCobrado.keySet().forEach(t -> turnos.putIfAbsent(t, BigDecimal.ZERO));

        return turnos.keySet().stream()
                .map(t -> aComisionVendedor(t.cajaId(), t.usuarioId(),
                        totalVendido.getOrDefault(t, BigDecimal.ZERO),
                        totalCobrado.getOrDefault(t, BigDecimal.ZERO)))
                .toList();
    }

    /**
     * Comisión total a pagar por un turno puntual (sumada entre todos los vendedores que
     * operaron esa caja), para ofrecerla al momento de cerrarla. Usa las mismas ventas y cobros
     * de cuenta corriente que ve el resumen de caja, agrupados por vendedor porque cada uno
     * puede tener un porcentaje de comisión distinto.
     */
    public BigDecimal comisionTotalDeTurno(Long cajaId) {
        Map<Long, BigDecimal> basePorUsuario = new LinkedHashMap<>();

        for (Venta venta : ventaService.listarPorCaja(cajaId)) {
            basePorUsuario.merge(venta.getUsuarioId(), venta.getTotal(), BigDecimal::add);
        }
        for (PagoCliente pago : cuentaCorrienteService.listarPorCaja(cajaId)) {
            basePorUsuario.merge(pago.getUsuarioId(), pago.getMonto(), BigDecimal::add);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entrada : basePorUsuario.entrySet()) {
            Usuario usuario = obtenerUsuario(entrada.getKey());
            total = total.add(calcularComision(entrada.getValue(), usuario.getPorcentajeComision()));
        }
        return total;
    }

    /**
     * Por cada orden de producción: comisión = cantidad producida × precio del producto ×
     * porcentaje de comisión del empleado que la confirmó.
     */
    public List<ComisionProduccionResponse> comisionesProduccion(Instant desde, Instant hasta) {
        return ordenProduccionService.listarEntrePeriodo(desde, hasta).stream()
                .map(this::aComisionProduccion)
                .toList();
    }

    private ComisionVendedorResponse aComisionVendedor(Long cajaId, Long usuarioId, BigDecimal totalVendido,
                                                        BigDecimal totalCobrado) {
        Usuario usuario = obtenerUsuario(usuarioId);
        BigDecimal base = totalVendido.add(totalCobrado);
        BigDecimal comision = calcularComision(base, usuario.getPorcentajeComision());

        return new ComisionVendedorResponse(
                cajaId, usuario.getId(), usuario.getNombre(), totalVendido, totalCobrado,
                usuario.getPorcentajeComision(), comision);
    }

    private ComisionProduccionResponse aComisionProduccion(OrdenProduccion orden) {
        Usuario usuario = obtenerUsuario(orden.getUsuarioId());
        Producto producto = productoService.obtenerPorId(orden.getProductoId());

        BigDecimal baseCalculo = orden.getCantidad().multiply(producto.getPrecioVenta());
        BigDecimal comision = calcularComision(baseCalculo, usuario.getPorcentajeComision());

        return new ComisionProduccionResponse(
                orden.getId(), usuario.getId(), usuario.getNombre(), producto.getId(), producto.getNombre(),
                orden.getCantidad(), producto.getPrecioVenta(), usuario.getPorcentajeComision(), comision);
    }

    private BigDecimal calcularComision(BigDecimal base, BigDecimal porcentaje) {
        if (porcentaje == null) {
            return BigDecimal.ZERO;
        }
        return base.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    private Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }
}
