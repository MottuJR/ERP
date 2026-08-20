package com.panaderia.erp.inventario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    List<MovimientoStock> findByItemTipoAndItemIdOrderByFechaDesc(ItemTipo itemTipo, Long itemId);
}
