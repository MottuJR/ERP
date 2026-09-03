package com.panaderia.erp.produccion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "receta_items")
public class RecetaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @Column(name = "insumo_id", nullable = false)
    private Long insumoId;

    /**
     * Cantidad de este insumo que lleva la tanda completa de la receta (no por unidad de
     * producto) — ver {@link Receta#getRendimiento()} para cuánto arroja esa tanda.
     */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    protected RecetaItem() {
    }

    public RecetaItem(Receta receta, Long insumoId, BigDecimal cantidad) {
        this.receta = receta;
        this.insumoId = insumoId;
        this.cantidad = cantidad;
    }

    public Long getId() {
        return id;
    }

    public Receta getReceta() {
        return receta;
    }

    public Long getInsumoId() {
        return insumoId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }
}
