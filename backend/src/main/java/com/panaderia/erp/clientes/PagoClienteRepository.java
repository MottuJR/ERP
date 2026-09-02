package com.panaderia.erp.clientes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PagoClienteRepository extends JpaRepository<PagoCliente, Long> {

    List<PagoCliente> findByClienteIdOrderByFechaDesc(Long clienteId);

    @Query("select coalesce(sum(p.monto), 0) from PagoCliente p where p.clienteId = :clienteId")
    BigDecimal sumMontoPorCliente(@Param("clienteId") Long clienteId);
}
