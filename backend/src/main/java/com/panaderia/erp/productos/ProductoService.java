package com.panaderia.erp.productos;

import com.panaderia.erp.core.auditoria.AccionAuditoria;
import com.panaderia.erp.core.auditoria.AuditoriaService;
import com.panaderia.erp.core.exception.ConflictoException;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import com.panaderia.erp.productos.dto.ActualizarProductoRequest;
import com.panaderia.erp.productos.dto.CrearProductoRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductoService {

    private static final String ENTIDAD = "Producto";

    private final ProductoRepository productoRepository;
    private final CategoriaService categoriaService;
    private final AuditoriaService auditoriaService;

    @Value("${app.stock.validar-disponibilidad:true}")
    private boolean validarDisponibilidad;

    public ProductoService(ProductoRepository productoRepository, CategoriaService categoriaService,
                            AuditoriaService auditoriaService) {
        this.productoRepository = productoRepository;
        this.categoriaService = categoriaService;
        this.auditoriaService = auditoriaService;
    }

    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrueConCategoria();
    }

    /**
     * Productos activos cuyo stock actual ya cayó al mínimo o por debajo. La usa el módulo de reportes.
     */
    public List<Producto> listarConStockCritico() {
        return productoRepository.findConStockCritico();
    }

    public Producto obtenerPorId(Long id) {
        return productoRepository.findByIdConCategoria(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + id));
    }

    public Producto obtenerPorCodigoBarras(String codigoBarras) {
        return productoRepository.findByCodigoBarrasConCategoria(codigoBarras)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay ningún producto con código de barras: " + codigoBarras));
    }

    public Producto obtenerPorCodigoPLU(String codigoPLU) {
        return productoRepository.findByCodigoPLUConCategoria(codigoPLU)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay ningún producto con código PLU: " + codigoPLU));
    }

    @Transactional
    public Producto crear(CrearProductoRequest request, String emailUsuario) {
        validarCodigos(request.seVendePorPeso(), request.codigoBarras(), request.codigoPLU());
        validarCodigosDisponibles(request.codigoBarras(), request.codigoPLU(), null);

        Categoria categoria = categoriaService.obtenerPorId(request.categoriaId());

        Producto producto = new Producto(
                request.nombre(),
                categoria,
                request.tipo(),
                request.seVendePorPeso(),
                request.precioVenta(),
                request.unidadMedida(),
                normalizar(request.codigoBarras()),
                normalizar(request.codigoPLU()),
                request.stockMinimo());

        producto = productoRepository.save(producto);

        auditoriaService.registrar(emailUsuario, ENTIDAD, producto.getId(), AccionAuditoria.CREAR,
                "Alta de \"%s\", precio inicial %s".formatted(producto.getNombre(), producto.getPrecioVenta()));

        return producto;
    }

    @Transactional
    public Producto actualizar(Long id, ActualizarProductoRequest request, String emailUsuario) {
        Producto producto = obtenerPorId(id);

        validarCodigos(request.seVendePorPeso(), request.codigoBarras(), request.codigoPLU());
        validarCodigosDisponibles(request.codigoBarras(), request.codigoPLU(), id);

        Categoria categoria = categoriaService.obtenerPorId(request.categoriaId());
        BigDecimal precioAnterior = producto.getPrecioVenta();

        producto.setNombre(request.nombre());
        producto.setCategoria(categoria);
        producto.setTipo(request.tipo());
        producto.setSeVendePorPeso(request.seVendePorPeso());
        producto.setPrecioVenta(request.precioVenta());
        producto.setUnidadMedida(request.unidadMedida());
        producto.setCodigoBarras(normalizar(request.codigoBarras()));
        producto.setCodigoPLU(normalizar(request.codigoPLU()));
        producto.setStockMinimo(request.stockMinimo());
        producto.setActivo(request.activo());

        if (precioAnterior.compareTo(request.precioVenta()) != 0) {
            auditoriaService.registrar(emailUsuario, ENTIDAD, producto.getId(), AccionAuditoria.ACTUALIZAR,
                    "Cambio de precio de \"%s\": %s -> %s".formatted(
                            producto.getNombre(), precioAnterior, request.precioVenta()));
        } else {
            auditoriaService.registrar(emailUsuario, ENTIDAD, producto.getId(), AccionAuditoria.ACTUALIZAR,
                    "Edición de \"%s\"".formatted(producto.getNombre()));
        }

        return producto;
    }

    @Transactional
    public void desactivar(Long id, String emailUsuario) {
        Producto producto = obtenerPorId(id);
        producto.setActivo(false);

        auditoriaService.registrar(emailUsuario, ENTIDAD, producto.getId(), AccionAuditoria.DESACTIVAR,
                "Baja de \"%s\"".formatted(producto.getNombre()));
    }

    /**
     * Punto único de mutación de stock de productos: lo usa el módulo de inventario
     * para reflejar ventas, compras, producción o ajustes manuales.
     */
    @Transactional
    public Producto ajustarStockActual(Long productoId, BigDecimal delta) {
        Producto producto = obtenerPorId(productoId);
        BigDecimal nuevoStock = producto.getStockActual().add(delta);

        if (validarDisponibilidad && nuevoStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictoException(
                    "Stock insuficiente para \"%s\": disponible %s, se intentó descontar %s"
                            .formatted(producto.getNombre(), producto.getStockActual(), delta.abs()));
        }

        producto.setStockActual(nuevoStock);
        return producto;
    }

    /**
     * Un producto vendido por peso siempre necesita un PLU (la balanza no puede imprimirle un
     * código de barras fijo, porque el código cambia con cada pesada). Un producto NO vendido
     * por peso también puede tener un PLU: la balanza (ver {@code EscaneoService}) puede
     * configurarse para que, con ese prefijo, imprima directamente una cantidad de unidades en
     * vez de un peso — por ejemplo, para contar facturas más rápido pesándolas en tanda.
     */
    private void validarCodigos(boolean seVendePorPeso, String codigoBarras, String codigoPLU) {
        if (seVendePorPeso && !StringUtils.hasText(codigoPLU)) {
            throw new ValidacionNegocioException(
                    "Un producto vendido por peso necesita un código PLU");
        }

        if (seVendePorPeso && StringUtils.hasText(codigoBarras)) {
            throw new ValidacionNegocioException(
                    "Un producto vendido por peso no debe tener código de barras fijo");
        }
    }

    private void validarCodigosDisponibles(String codigoBarras, String codigoPLU, Long idExcluido) {
        if (StringUtils.hasText(codigoBarras)) {
            productoRepository.findByCodigoBarrasConCategoria(codigoBarras)
                    .filter(p -> !p.getId().equals(idExcluido))
                    .ifPresent(p -> {
                        throw new ConflictoException("Ya existe un producto con ese código de barras");
                    });
        }

        if (StringUtils.hasText(codigoPLU)) {
            productoRepository.findByCodigoPLUConCategoria(codigoPLU)
                    .filter(p -> !p.getId().equals(idExcluido))
                    .ifPresent(p -> {
                        throw new ConflictoException("Ya existe un producto con ese código PLU");
                    });
        }
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
