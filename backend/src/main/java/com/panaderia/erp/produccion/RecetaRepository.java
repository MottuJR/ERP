package com.panaderia.erp.produccion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecetaRepository extends JpaRepository<Receta, Long> {

    Optional<Receta> findByProductoId(Long productoId);

    boolean existsByProductoId(Long productoId);
}
