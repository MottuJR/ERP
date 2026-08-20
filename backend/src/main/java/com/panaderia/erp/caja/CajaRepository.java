package com.panaderia.erp.caja;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    Optional<Caja> findFirstByEstado(EstadoCaja estado);
}
