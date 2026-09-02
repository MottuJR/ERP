package com.panaderia.erp.inventario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    @Query("select i from Insumo i where i.stockActual <= i.stockMinimo")
    List<Insumo> findConStockCritico();
}
