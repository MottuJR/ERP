package com.panaderia.erp.comisiones;

import com.panaderia.erp.comisiones.dto.ComisionProduccionResponse;
import com.panaderia.erp.comisiones.dto.ComisionVendedorResponse;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import com.panaderia.erp.produccion.OrdenProduccion;
import com.panaderia.erp.produccion.OrdenProduccionService;
import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import com.panaderia.erp.ventas.VentaService;
import com.panaderia.erp.ventas.dto.VentaTurnoResumen;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Calcula comisiones on-demand (sección 7 del documento de diseño): no se persisten como
 * movimientos todavía, solo se leen Venta, OrdenProduccion, Caja y el porcentaje de cada
 * usuario para armar el reporte.
 */
@Service
public class ComisionesService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final VentaService ventaService;
    private final OrdenProduccionService ordenProduccionService;
    private final UsuarioRepository usuarioRepository;
    private final ProductoService productoService;

    public ComisionesService(VentaService ventaService, OrdenProduccionService ordenProduccionService,
                              UsuarioRepository usuarioRepository, ProductoService productoService) {
        this.ventaService = ventaService;
        this.ordenProduccionService = ordenProduccionService;
        this.usuarioRepository = usuarioRepository;
        this.productoService = productoService;
    }

    /**
     * Por cada turno (Caja) y vendedor: comisión = total vendido por ese vendedor en ese turno
     * × su porcentaje de comisión.
     */
    public List<ComisionVendedorResponse> comisionesVendedores(Instant desde, Instant hasta) {
        return ventaService.totalVendidoPorTurnoYUsuario(desde, hasta).stream()
                .map(this::aComisionVendedor)
                .toList();
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

    private ComisionVendedorResponse aComisionVendedor(VentaTurnoResumen resumen) {
        Usuario usuario = obtenerUsuario(resumen.usuarioId());
        BigDecimal comision = calcularComision(resumen.totalVendido(), usuario.getPorcentajeComision());

        return new ComisionVendedorResponse(
                resumen.cajaId(), usuario.getId(), usuario.getNombre(), resumen.totalVendido(),
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
