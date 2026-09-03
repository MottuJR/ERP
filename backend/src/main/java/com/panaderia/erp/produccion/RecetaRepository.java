package com.panaderia.erp.produccion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RecetaRepository extends JpaRepository<Receta, Long> {

    // JOIN FETCH de items: con spring.jpa.open-in-view=false, la sesión ya se cerró para cuando
    // el que llama (fuera de una transacción, como el GET de detalle o el cálculo de margen de
    // reportes) recorre receta.getItems() — sin este fetch explícito rompe con
    // LazyInitializationException. Mismo patrón que ProductoRepository con su categoría.
    @Query("select r from Receta r join fetch r.items where r.productoId = :productoId")
    Optional<Receta> findByProductoId(Long productoId);

    boolean existsByProductoId(Long productoId);
}
