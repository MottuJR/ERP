package com.panaderia.erp.ventas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    @Query("""
            select d.productoId as productoId, sum(d.cantidad) as cantidadVendida, sum(d.subtotal) as montoTotal
            from DetalleVenta d
            where d.venta.fecha between :desde and :hasta
            group by d.productoId
            order by sum(d.cantidad) desc
            """)
    List<ProductoVendidoProjection> productosMasVendidos(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    interface ProductoVendidoProjection {
        Long getProductoId();

        BigDecimal getCantidadVendida();

        BigDecimal getMontoTotal();
    }
}
