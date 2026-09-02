package com.panaderia.erp.ventas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByFechaBetween(Instant desde, Instant hasta);

    @Query("""
            select coalesce(sum(v.total), 0)
            from Venta v
            where v.clienteId = :clienteId and v.medioPago = com.panaderia.erp.ventas.MedioPago.CUENTA_CORRIENTE
            """)
    BigDecimal sumTotalCuentaCorrientePorCliente(@Param("clienteId") Long clienteId);

    @Query("""
            select v.cajaId as cajaId, v.usuarioId as usuarioId, sum(v.total) as totalVendido
            from Venta v
            where v.cajaId is not null and v.fecha between :desde and :hasta
            group by v.cajaId, v.usuarioId
            """)
    List<VentaPorTurnoProjection> totalVendidoPorTurnoYUsuario(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    interface VentaPorTurnoProjection {
        Long getCajaId();

        Long getUsuarioId();

        BigDecimal getTotalVendido();
    }
}
