package com.panaderia.erp.produccion;

import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import com.panaderia.erp.inventario.InventarioService;
import com.panaderia.erp.produccion.dto.CrearOrdenProduccionRequest;
import com.panaderia.erp.produccion.dto.OrdenProduccionResponse;
import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class OrdenProduccionService {

    private final OrdenProduccionRepository ordenProduccionRepository;
    private final RecetaRepository recetaRepository;
    private final ProductoService productoService;
    private final InventarioService inventarioService;
    private final UsuarioRepository usuarioRepository;

    public OrdenProduccionService(OrdenProduccionRepository ordenProduccionRepository,
                                   RecetaRepository recetaRepository,
                                   ProductoService productoService,
                                   InventarioService inventarioService,
                                   UsuarioRepository usuarioRepository) {
        this.ordenProduccionRepository = ordenProduccionRepository;
        this.recetaRepository = recetaRepository;
        this.productoService = productoService;
        this.inventarioService = inventarioService;
        this.usuarioRepository = usuarioRepository;
    }

    public OrdenProduccionResponse obtenerPorId(Long id) {
        OrdenProduccion orden = ordenProduccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden de producción no encontrada: " + id));
        Producto producto = productoService.obtenerPorId(orden.getProductoId());
        return aResponse(orden, producto.getNombre());
    }

    /**
     * Confirma la orden: descuenta de una vez cada insumo de la receta (cantidad de la receta × cantidad
     * a producir) y suma el stock del producto terminado. Todo en una transacción: si falta stock de
     * cualquier insumo, no se descuenta ni se suma nada.
     */
    @Transactional
    public OrdenProduccionResponse confirmar(CrearOrdenProduccionRequest request, String emailUsuario) {
        Producto producto = productoService.obtenerPorId(request.productoId());

        if (!producto.isActivo()) {
            throw new ValidacionNegocioException("El producto \"" + producto.getNombre() + "\" no está activo");
        }

        Receta receta = recetaRepository.findByProductoId(producto.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El producto \"" + producto.getNombre() + "\" no tiene una receta cargada"));

        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado"));

        OrdenProduccion orden = ordenProduccionRepository.save(
                new OrdenProduccion(producto.getId(), request.cantidad(), usuario.getId()));

        for (RecetaItem item : receta.getItems()) {
            BigDecimal cantidadNecesaria = item.getCantidad().multiply(request.cantidad());
            inventarioService.registrarSalidaInsumoPorProduccion(item.getInsumoId(), cantidadNecesaria, orden.getId());
        }

        inventarioService.registrarEntradaProductoPorProduccion(producto.getId(), request.cantidad(), orden.getId());

        return aResponse(orden, producto.getNombre());
    }

    /**
     * Órdenes de producción confirmadas en un período. La usa el módulo de comisiones.
     */
    public List<OrdenProduccion> listarEntrePeriodo(Instant desde, Instant hasta) {
        return ordenProduccionRepository.findByFechaBetween(desde, hasta);
    }

    private OrdenProduccionResponse aResponse(OrdenProduccion orden, String productoNombre) {
        return new OrdenProduccionResponse(
                orden.getId(), orden.getProductoId(), productoNombre, orden.getCantidad(),
                orden.getFecha(), orden.getEstado(), orden.getUsuarioId());
    }
}
