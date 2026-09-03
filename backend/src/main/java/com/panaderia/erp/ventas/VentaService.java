package com.panaderia.erp.ventas;

import com.panaderia.erp.clientes.ClienteService;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import com.panaderia.erp.inventario.InventarioService;
import com.panaderia.erp.productos.Producto;
import com.panaderia.erp.productos.ProductoService;
import com.panaderia.erp.ventas.dto.ConfirmarVentaRequest;
import com.panaderia.erp.ventas.dto.DetalleVentaResponse;
import com.panaderia.erp.ventas.dto.EscaneoResponse;
import com.panaderia.erp.ventas.dto.ItemVentaRequest;
import com.panaderia.erp.ventas.dto.ProductoVendidoResumen;
import com.panaderia.erp.ventas.dto.VentaPorMedioPagoResumen;
import com.panaderia.erp.ventas.dto.VentaResponse;
import com.panaderia.erp.ventas.dto.VentaTurnoResumen;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final EscaneoService escaneoService;
    private final ProductoService productoService;
    private final InventarioService inventarioService;
    private final UsuarioRepository usuarioRepository;
    private final ClienteService clienteService;

    public VentaService(VentaRepository ventaRepository,
                         DetalleVentaRepository detalleVentaRepository,
                         EscaneoService escaneoService,
                         ProductoService productoService,
                         InventarioService inventarioService,
                         UsuarioRepository usuarioRepository,
                         ClienteService clienteService) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.escaneoService = escaneoService;
        this.productoService = productoService;
        this.inventarioService = inventarioService;
        this.usuarioRepository = usuarioRepository;
        this.clienteService = clienteService;
    }

    public EscaneoResponse escanear(String codigo, BigDecimal cantidadManual) {
        EscaneoService.ItemResuelto resuelto = escaneoService.resolver(codigo, cantidadManual);
        return aEscaneoResponse(resuelto);
    }

    public VentaResponse obtenerPorId(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada: " + id));
        return aVentaResponse(venta, resolverProductos(venta));
    }

    @Transactional
    public VentaResponse confirmarVenta(ConfirmarVentaRequest request, String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado"));

        if (request.medioPago() == MedioPago.CUENTA_CORRIENTE) {
            if (request.clienteId() == null) {
                throw new ValidacionNegocioException(
                        "Una venta a cuenta corriente necesita indicar el cliente");
            }
            clienteService.validarTieneCuentaCorriente(request.clienteId());
        }

        Venta venta = new Venta(request.clienteId(), usuario.getId(), request.cajaId(), request.medioPago());
        List<EscaneoService.ItemResuelto> resueltos = new ArrayList<>();

        for (ItemVentaRequest item : request.items()) {
            EscaneoService.ItemResuelto resuelto = resolverItem(item);

            if (!resuelto.producto().isActivo()) {
                throw new ValidacionNegocioException(
                        "El producto \"" + resuelto.producto().getNombre() + "\" no está activo");
            }

            if (resuelto.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidacionNegocioException(
                        "La cantidad de \"" + resuelto.producto().getNombre() + "\" debe ser mayor a cero");
            }

            resueltos.add(resuelto);
            venta.agregarDetalle(resuelto.producto().getId(), resuelto.cantidad(), resuelto.producto().getPrecioVenta());
        }

        venta = ventaRepository.save(venta);

        for (EscaneoService.ItemResuelto resuelto : resueltos) {
            inventarioService.registrarSalidaPorVenta(resuelto.producto().getId(), resuelto.cantidad(), venta.getId());
        }

        return aVentaResponse(venta, resueltos.stream().map(EscaneoService.ItemResuelto::producto).toList());
    }

    /**
     * Total vendido a un cliente con medioPago CUENTA_CORRIENTE. Lo usa el módulo de clientes
     * para calcular el saldo de cuenta corriente.
     */
    public BigDecimal totalVendidoACuentaCorriente(Long clienteId) {
        return ventaRepository.sumTotalCuentaCorrientePorCliente(clienteId);
    }

    /**
     * Ventas confirmadas en un período. La usa el módulo de reportes.
     */
    public List<Venta> listarEntrePeriodo(Instant desde, Instant hasta) {
        return ventaRepository.findByFechaBetween(desde, hasta);
    }

    /**
     * Total vendido por cada (turno, vendedor) en un período — solo ventas con caja asignada.
     * La usa el módulo de comisiones para calcular la comisión de vendedores.
     */
    public List<VentaTurnoResumen> totalVendidoPorTurnoYUsuario(Instant desde, Instant hasta) {
        return ventaRepository.totalVendidoPorTurnoYUsuario(desde, hasta).stream()
                .map(p -> new VentaTurnoResumen(p.getCajaId(), p.getUsuarioId(), p.getTotalVendido()))
                .toList();
    }

    /**
     * Total vendido y cantidad de ventas por medio de pago dentro de una caja puntual.
     * La usa el módulo de caja para el resumen de un turno cerrado.
     */
    public List<VentaPorMedioPagoResumen> resumenPorMedioPago(Long cajaId) {
        return ventaRepository.totalPorMedioPago(cajaId).stream()
                .map(p -> new VentaPorMedioPagoResumen(p.getMedioPago(), p.getTotal(), p.getCantidad()))
                .toList();
    }

    /**
     * Cantidad y monto vendido por producto en un período, ordenado de más a menos vendido.
     * La usa el módulo de reportes.
     */
    public List<ProductoVendidoResumen> productosMasVendidos(Instant desde, Instant hasta) {
        return detalleVentaRepository.productosMasVendidos(desde, hasta).stream()
                .map(p -> new ProductoVendidoResumen(p.getProductoId(), p.getCantidadVendida(), p.getMontoTotal()))
                .toList();
    }

    private EscaneoService.ItemResuelto resolverItem(ItemVentaRequest item) {
        boolean tieneCodigo = StringUtils.hasText(item.codigoEscaneado());
        boolean tieneProductoManual = item.productoId() != null;

        if (tieneCodigo == tieneProductoManual) {
            throw new ValidacionNegocioException(
                    "Cada ítem del carrito debe traer o un código escaneado, o un productoId + cantidad, pero no ambos");
        }

        if (tieneCodigo) {
            return escaneoService.resolver(item.codigoEscaneado(), item.cantidad());
        }

        if (item.cantidad() == null || item.cantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionNegocioException("La cantidad es obligatoria al agregar un producto manualmente");
        }

        Producto producto = productoService.obtenerPorId(item.productoId());
        return new EscaneoService.ItemResuelto(producto, item.cantidad());
    }

    private List<Producto> resolverProductos(Venta venta) {
        return venta.getDetalles().stream()
                .map(detalle -> productoService.obtenerPorId(detalle.getProductoId()))
                .toList();
    }

    private VentaResponse aVentaResponse(Venta venta, List<Producto> productos) {
        List<DetalleVentaResponse> detalles = new ArrayList<>();

        for (int i = 0; i < venta.getDetalles().size(); i++) {
            DetalleVenta detalle = venta.getDetalles().get(i);
            Producto producto = productos.get(i);
            detalles.add(new DetalleVentaResponse(
                    detalle.getId(), detalle.getProductoId(), producto.getNombre(),
                    detalle.getCantidad(), detalle.getPrecioUnitario(), detalle.getSubtotal()));
        }

        return new VentaResponse(
                venta.getId(), venta.getFecha(), venta.getClienteId(), venta.getUsuarioId(), venta.getCajaId(),
                venta.getTotal(), venta.getMedioPago(), venta.getEstado(), detalles);
    }

    private EscaneoResponse aEscaneoResponse(EscaneoService.ItemResuelto resuelto) {
        Producto producto = resuelto.producto();
        BigDecimal subtotal = producto.getPrecioVenta().multiply(resuelto.cantidad());

        return new EscaneoResponse(
                producto.getId(), producto.getNombre(), producto.isSeVendePorPeso(),
                resuelto.cantidad(), producto.getPrecioVenta(), subtotal);
    }
}
