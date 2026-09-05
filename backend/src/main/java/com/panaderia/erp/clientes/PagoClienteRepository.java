package com.panaderia.erp.clientes;

import com.panaderia.erp.ventas.MedioPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PagoClienteRepository extends JpaRepository<PagoCliente, Long> {

    List<PagoCliente> findByClienteIdOrderByFechaDesc(Long clienteId);

    List<PagoCliente> findByCajaIdOrderByFechaDesc(Long cajaId);

    @Query("select coalesce(sum(p.monto), 0) from PagoCliente p where p.clienteId = :clienteId")
    BigDecimal sumMontoPorCliente(@Param("clienteId") Long clienteId);

    @Query("""
            select p.cajaId as cajaId, p.usuarioId as usuarioId, sum(p.monto) as totalPagado
            from PagoCliente p
            where p.cajaId is not null and p.fecha between :desde and :hasta
            group by p.cajaId, p.usuarioId
            """)
    List<PagoPorTurnoProjection> totalPagadoPorTurnoYUsuario(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            select p.medioPago as medioPago, sum(p.monto) as total, count(p) as cantidad
            from PagoCliente p
            where p.cajaId = :cajaId
            group by p.medioPago
            """)
    List<PagoPorMedioPagoProjection> totalPorMedioPago(@Param("cajaId") Long cajaId);

    interface PagoPorTurnoProjection {
        Long getCajaId();

        Long getUsuarioId();

        BigDecimal getTotalPagado();
    }

    interface PagoPorMedioPagoProjection {
        MedioPago getMedioPago();

        BigDecimal getTotal();

        Long getCantidad();
    }
}
