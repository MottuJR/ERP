package com.panaderia.erp.produccion;

import com.panaderia.erp.core.audit.Auditable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "recetas")
public class Receta extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false, unique = true)
    private Long productoId;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<RecetaItem> items = new ArrayList<>();

    protected Receta() {
    }

    public Receta(Long productoId) {
        this.productoId = productoId;
    }

    public void agregarItem(Long insumoId, BigDecimal cantidad) {
        items.add(new RecetaItem(this, insumoId, cantidad));
    }

    public void limpiarItems() {
        items.clear();
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public List<RecetaItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
