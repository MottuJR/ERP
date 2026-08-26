package com.panaderia.erp.compras;

import com.panaderia.erp.compras.dto.CompraResponse;
import com.panaderia.erp.compras.dto.ConfirmarCompraRequest;
import com.panaderia.erp.compras.dto.DetalleCompraResponse;
import com.panaderia.erp.compras.dto.ItemCompraRequest;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.inventario.Insumo;
import com.panaderia.erp.inventario.InventarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorService proveedorService;
    private final InventarioService inventarioService;

    public CompraService(CompraRepository compraRepository, ProveedorService proveedorService,
                          InventarioService inventarioService) {
        this.compraRepository = compraRepository;
        this.proveedorService = proveedorService;
        this.inventarioService = inventarioService;
    }

    public CompraResponse obtenerPorId(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + id));
        Proveedor proveedor = proveedorService.obtenerPorId(compra.getProveedorId());
        return aResponse(compra, proveedor, resolverInsumos(compra));
    }

    /**
     * Confirma la compra: suma el stock de cada insumo recibido y actualiza su costo unitario al
     * último precio pagado. Todo en una transacción.
     */
    @Transactional
    public CompraResponse confirmarCompra(ConfirmarCompraRequest request) {
        Proveedor proveedor = proveedorService.obtenerPorId(request.proveedorId());

        Compra compra = new Compra(proveedor.getId());
        List<Insumo> insumos = new ArrayList<>();

        for (ItemCompraRequest item : request.items()) {
            Insumo insumo = inventarioService.obtenerInsumoPorId(item.insumoId());
            insumos.add(insumo);
            compra.agregarDetalle(insumo.getId(), item.cantidad(), item.costoUnitario());
        }

        compra = compraRepository.save(compra);

        for (ItemCompraRequest item : request.items()) {
            inventarioService.registrarEntradaInsumoPorCompra(
                    item.insumoId(), item.cantidad(), item.costoUnitario(), compra.getId());
        }

        return aResponse(compra, proveedor, insumos);
    }

    private List<Insumo> resolverInsumos(Compra compra) {
        return compra.getDetalles().stream()
                .map(detalle -> inventarioService.obtenerInsumoPorId(detalle.getInsumoId()))
                .toList();
    }

    private CompraResponse aResponse(Compra compra, Proveedor proveedor, List<Insumo> insumos) {
        List<DetalleCompraResponse> detalles = new ArrayList<>();

        for (int i = 0; i < compra.getDetalles().size(); i++) {
            DetalleCompra detalle = compra.getDetalles().get(i);
            Insumo insumo = insumos.get(i);
            detalles.add(new DetalleCompraResponse(
                    detalle.getId(), detalle.getInsumoId(), insumo.getNombre(),
                    detalle.getCantidad(), detalle.getCostoUnitario(), detalle.getSubtotal()));
        }

        return new CompraResponse(
                compra.getId(), compra.getProveedorId(), proveedor.getNombre(), compra.getFecha(),
                compra.getTotal(), compra.getEstado(), detalles);
    }
}
