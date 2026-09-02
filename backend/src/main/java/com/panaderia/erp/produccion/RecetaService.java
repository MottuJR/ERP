package com.panaderia.erp.produccion;

import com.panaderia.erp.core.exception.ConflictoException;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import com.panaderia.erp.inventario.Insumo;
import com.panaderia.erp.inventario.InventarioService;
import com.panaderia.erp.produccion.dto.ActualizarRecetaRequest;
import com.panaderia.erp.produccion.dto.CrearRecetaRequest;
import com.panaderia.erp.produccion.dto.RecetaItemRequest;
import com.panaderia.erp.produccion.dto.RecetaItemResponse;
import com.panaderia.erp.produccion.dto.RecetaResponse;
import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import com.panaderia.erp.productos.TipoProducto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecetaService {

    private final RecetaRepository recetaRepository;
    private final ProductoService productoService;
    private final InventarioService inventarioService;

    public RecetaService(RecetaRepository recetaRepository, ProductoService productoService,
                          InventarioService inventarioService) {
        this.recetaRepository = recetaRepository;
        this.productoService = productoService;
        this.inventarioService = inventarioService;
    }

    public Receta obtenerPorProducto(Long productoId) {
        return recetaRepository.findByProductoId(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El producto " + productoId + " no tiene una receta cargada"));
    }

    /**
     * Igual que {@link #obtenerPorProducto}, pero sin lanzar si no hay receta cargada. La usa
     * el módulo de reportes para calcular el margen solo de los productos que sí tienen receta.
     */
    public Optional<Receta> buscarPorProducto(Long productoId) {
        return recetaRepository.findByProductoId(productoId);
    }

    /**
     * Costo total de insumos de una receta (suma de cantidad × costoUnitario de cada ítem).
     * La usa el módulo de reportes para calcular el margen por producto.
     */
    public BigDecimal costoInsumos(Receta receta) {
        return receta.getItems().stream()
                .map(item -> {
                    Insumo insumo = inventarioService.obtenerInsumoPorId(item.getInsumoId());
                    return insumo.getCostoUnitario().multiply(item.getCantidad());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Receta crear(CrearRecetaRequest request) {
        Producto producto = validarProductoElaborado(request.productoId());

        if (recetaRepository.existsByProductoId(producto.getId())) {
            throw new ConflictoException("El producto \"" + producto.getNombre() + "\" ya tiene una receta cargada");
        }

        validarItems(request.items());

        Receta receta = new Receta(producto.getId());
        request.items().forEach(item -> receta.agregarItem(item.insumoId(), item.cantidad()));

        return recetaRepository.save(receta);
    }

    @Transactional
    public Receta actualizar(Long productoId, ActualizarRecetaRequest request) {
        Receta receta = obtenerPorProducto(productoId);

        validarItems(request.items());

        receta.limpiarItems();
        request.items().forEach(item -> receta.agregarItem(item.insumoId(), item.cantidad()));

        return receta;
    }

    public RecetaResponse aResponse(Receta receta) {
        Producto producto = productoService.obtenerPorId(receta.getProductoId());

        List<RecetaItemResponse> items = receta.getItems().stream()
                .map(item -> {
                    Insumo insumo = inventarioService.obtenerInsumoPorId(item.getInsumoId());
                    return new RecetaItemResponse(
                            insumo.getId(), insumo.getNombre(), insumo.getUnidadMedida().name(), item.getCantidad());
                })
                .toList();

        return new RecetaResponse(receta.getId(), producto.getId(), producto.getNombre(), items);
    }

    private void validarItems(List<RecetaItemRequest> items) {
        Set<Long> insumoIds = items.stream().map(RecetaItemRequest::insumoId).collect(Collectors.toSet());

        if (insumoIds.size() != items.size()) {
            throw new ValidacionNegocioException("La receta no puede repetir el mismo insumo en más de un ítem");
        }

        for (Long insumoId : insumoIds) {
            inventarioService.obtenerInsumoPorId(insumoId);
        }
    }

    private Producto validarProductoElaborado(Long productoId) {
        Producto producto = productoService.obtenerPorId(productoId);

        if (producto.getTipo() != TipoProducto.ELABORADO) {
            throw new ValidacionNegocioException(
                    "Solo los productos de tipo ELABORADO pueden tener receta (\""
                            + producto.getNombre() + "\" es de reventa)");
        }

        return producto;
    }
}
