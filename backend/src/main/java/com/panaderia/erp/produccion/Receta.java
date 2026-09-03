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

    /**
     * Cuánto rinde la tanda tal como está cargada la receta, en la unidad de venta del producto
     * (kg si se vende por peso, unidades si se vende por unidad). Por ejemplo: una receta de
     * medialunas puede llevar los insumos de 1 kg de masa y rendir 40 unidades — los ítems de la
     * receta son el total de esa tanda, no "por unidad de producto". Con rendimiento = 1 (el
     * valor por defecto de las recetas ya cargadas) el comportamiento es el de antes: los ítems
     * se interpretan directamente como cantidad por unidad de producto.
     */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal rendimiento;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<RecetaItem> items = new ArrayList<>();

    protected Receta() {
    }

    public Receta(Long productoId, BigDecimal rendimiento) {
        this.productoId = productoId;
        this.rendimiento = rendimiento;
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

    public BigDecimal getRendimiento() {
        return rendimiento;
    }

    public void setRendimiento(BigDecimal rendimiento) {
        this.rendimiento = rendimiento;
    }

    public List<RecetaItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
