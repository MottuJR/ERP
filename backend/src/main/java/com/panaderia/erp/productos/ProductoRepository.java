package com.panaderia.erp.productos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigoBarras(String codigoBarras);

    Optional<Producto> findByCodigoPLU(String codigoPLU);

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByCodigoPLU(String codigoPLU);

    List<Producto> findByActivoTrue();
}
