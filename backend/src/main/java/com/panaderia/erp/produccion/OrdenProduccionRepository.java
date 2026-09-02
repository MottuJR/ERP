package com.panaderia.erp.produccion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OrdenProduccionRepository extends JpaRepository<OrdenProduccion, Long> {

    List<OrdenProduccion> findByFechaBetween(Instant desde, Instant hasta);
}
