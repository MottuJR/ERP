package com.panaderia.erp.inventario;

import com.panaderia.erp.core.auditoria.AccionAuditoria;
import com.panaderia.erp.core.auditoria.AuditoriaService;
import com.panaderia.erp.core.exception.ConflictoException;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import com.panaderia.erp.inventario.dto.InsumoRequest;
import com.panaderia.erp.inventario.dto.MovimientoManualRequest;
import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Punto central por el que pasa todo cambio de stock (de productos o insumos),
 * para que {@link MovimientoStock} quede como registro completo de trazabilidad.
 * Otros módulos (ventas, y en el futuro producción/compras) piden acá el descuento
 * o alta de stock en vez de tocar Producto/Insumo directamente.
 */
@Service
public class InventarioService {

    private final InsumoRepository insumoRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final ProductoService productoService;
    private final AuditoriaService auditoriaService;

    @Value("${app.stock.validar-disponibilidad:true}")
    private boolean validarDisponibilidad;

    public InventarioService(InsumoRepository insumoRepository,
                              MovimientoStockRepository movimientoStockRepository,
                              ProductoService productoService,
                              AuditoriaService auditoriaService) {
        this.insumoRepository = insumoRepository;
        this.movimientoStockRepository = movimientoStockRepository;
        this.productoService = productoService;
        this.auditoriaService = auditoriaService;
    }

    public List<Insumo> listarInsumos() {
        return insumoRepository.findAll();
    }

    public Insumo obtenerInsumoPorId(Long id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Insumo no encontrado: " + id));
    }

    /**
     * Insumos cuyo stock actual ya cayó al mínimo o por debajo. La usa el módulo de reportes.
     */
    public List<Insumo> listarConStockCritico() {
        return insumoRepository.findConStockCritico();
    }

    @Transactional
    public Insumo crearInsumo(InsumoRequest request) {
        return insumoRepository.save(new Insumo(
                request.nombre(), request.unidadMedida(), request.stockMinimo(), request.costoUnitario()));
    }

    /**
     * No toca stockActual (eso solo cambia vía movimientos) ni requiere auditoría propia: es
     * una edición de datos maestros, no un ajuste de stock.
     */
    @Transactional
    public Insumo actualizarInsumo(Long id, InsumoRequest request) {
        Insumo insumo = obtenerInsumoPorId(id);
        insumo.setNombre(request.nombre());
        insumo.setUnidadMedida(request.unidadMedida());
        insumo.setStockMinimo(request.stockMinimo());
        insumo.setCostoUnitario(request.costoUnitario());
        return insumo;
    }

    public List<MovimientoStock> listarMovimientos(ItemTipo itemTipo, Long itemId) {
        return movimientoStockRepository.findByItemTipoAndItemIdOrderByFechaDesc(itemTipo, itemId);
    }

    @Transactional
    public MovimientoStock registrarMovimientoManual(MovimientoManualRequest request, String emailUsuario) {
        validarSigno(request.tipo(), request.cantidad());

        MovimientoStock movimiento = switch (request.itemTipo()) {
            case PRODUCTO -> aplicarMovimiento(
                    request.tipo(), ItemTipo.PRODUCTO, request.itemId(), request.cantidad(), request.motivo(), null);
            case INSUMO -> aplicarMovimiento(
                    request.tipo(), ItemTipo.INSUMO, request.itemId(), request.cantidad(), request.motivo(), null);
        };

        auditoriaService.registrar(emailUsuario, request.itemTipo().name(), request.itemId(),
                AccionAuditoria.AJUSTE_STOCK,
                "%s manual de %s: motivo \"%s\"".formatted(request.tipo(), request.cantidad(), request.motivo()));

        return movimiento;
    }

    /**
     * Descuenta stock de un producto por una venta confirmada. Lo usa el módulo de ventas.
     */
    @Transactional
    public MovimientoStock registrarSalidaPorVenta(Long productoId, BigDecimal cantidad, Long ventaId) {
        return aplicarMovimiento(
                TipoMovimiento.SALIDA, ItemTipo.PRODUCTO, productoId, cantidad.negate(), "Venta", ventaId);
    }

    /**
     * Descuenta el insumo que consume una orden de producción (según su receta). Lo usa el módulo de producción.
     */
    @Transactional
    public MovimientoStock registrarSalidaInsumoPorProduccion(Long insumoId, BigDecimal cantidad, Long ordenProduccionId) {
        return aplicarMovimiento(
                TipoMovimiento.SALIDA, ItemTipo.INSUMO, insumoId, cantidad.negate(), "Producción", ordenProduccionId);
    }

    /**
     * Suma el producto terminado que resulta de una orden de producción. Lo usa el módulo de producción.
     */
    @Transactional
    public MovimientoStock registrarEntradaProductoPorProduccion(Long productoId, BigDecimal cantidad, Long ordenProduccionId) {
        return aplicarMovimiento(
                TipoMovimiento.ENTRADA, ItemTipo.PRODUCTO, productoId, cantidad, "Producción", ordenProduccionId);
    }

    /**
     * Suma el insumo recibido en una compra y actualiza su costo unitario al último precio pagado.
     * Lo usa el módulo de compras.
     */
    @Transactional
    public MovimientoStock registrarEntradaInsumoPorCompra(Long insumoId, BigDecimal cantidad,
                                                            BigDecimal costoUnitario, Long compraId) {
        Insumo insumo = obtenerInsumoPorId(insumoId);
        insumo.setCostoUnitario(costoUnitario);
        return aplicarMovimiento(TipoMovimiento.ENTRADA, ItemTipo.INSUMO, insumoId, cantidad, "Compra", compraId);
    }

    private MovimientoStock aplicarMovimiento(TipoMovimiento tipo, ItemTipo itemTipo, Long itemId,
                                               BigDecimal delta, String motivo, Long referenciaId) {
        if (itemTipo == ItemTipo.PRODUCTO) {
            productoService.ajustarStockActual(itemId, delta);
        } else {
            ajustarStockInsumo(itemId, delta);
        }

        MovimientoStock movimiento = new MovimientoStock(tipo, itemTipo, itemId, delta.abs(), motivo, referenciaId);
        return movimientoStockRepository.save(movimiento);
    }

    private void ajustarStockInsumo(Long insumoId, BigDecimal delta) {
        Insumo insumo = obtenerInsumoPorId(insumoId);
        BigDecimal nuevoStock = insumo.getStockActual().add(delta);

        if (validarDisponibilidad && nuevoStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictoException(
                    "Stock insuficiente para el insumo \"%s\": disponible %s, se intentó descontar %s"
                            .formatted(insumo.getNombre(), insumo.getStockActual(), delta.abs()));
        }

        insumo.setStockActual(nuevoStock);
    }

    private void validarSigno(TipoMovimiento tipo, BigDecimal cantidad) {
        if (cantidad.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidacionNegocioException("La cantidad del movimiento no puede ser cero");
        }

        boolean esPositivo = cantidad.compareTo(BigDecimal.ZERO) > 0;

        switch (tipo) {
            case ENTRADA -> {
                if (!esPositivo) {
                    throw new ValidacionNegocioException("Un movimiento de tipo ENTRADA debe tener cantidad positiva");
                }
            }
            case SALIDA, MERMA -> {
                if (esPositivo) {
                    throw new ValidacionNegocioException(
                            "Un movimiento de tipo " + tipo + " debe tener cantidad negativa");
                }
            }
            case AJUSTE -> {
                // un ajuste puede corregir stock hacia arriba o hacia abajo
            }
        }
    }
}
