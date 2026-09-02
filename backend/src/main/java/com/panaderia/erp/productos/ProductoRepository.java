package com.panaderia.erp.productos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Traen la categoría con JOIN FETCH: con spring.jpa.open-in-view=false, la sesión de
    // Hibernate se cierra al terminar el método del service, así que cualquier acceso lazy a
    // producto.getCategoria() hecho después (por ejemplo al armar el DTO en el controller)
    // rompe con LazyInitializationException si no se trajo ya inicializada acá.

    @Query("select p from Producto p join fetch p.categoria where p.id = :id")
    Optional<Producto> findByIdConCategoria(Long id);

    @Query("select p from Producto p join fetch p.categoria where p.codigoBarras = :codigoBarras")
    Optional<Producto> findByCodigoBarrasConCategoria(String codigoBarras);

    @Query("select p from Producto p join fetch p.categoria where p.codigoPLU = :codigoPLU")
    Optional<Producto> findByCodigoPLUConCategoria(String codigoPLU);

    @Query("select p from Producto p join fetch p.categoria where p.activo = true")
    List<Producto> findByActivoTrueConCategoria();

    @Query("select p from Producto p join fetch p.categoria where p.activo = true and p.stockActual <= p.stockMinimo")
    List<Producto> findConStockCritico();

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByCodigoPLU(String codigoPLU);
}
