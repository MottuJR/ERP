package com.panaderia.erp.inventario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "movimientos_stock")
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_tipo", nullable = false, length = 20)
    private ItemTipo itemTipo;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(nullable = false)
    private Instant fecha;

    @Column(length = 255)
    private String motivo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    protected MovimientoStock() {
    }

    public MovimientoStock(TipoMovimiento tipo, ItemTipo itemTipo, Long itemId, BigDecimal cantidad,
                            String motivo, Long referenciaId) {
        this.tipo = tipo;
        this.itemTipo = itemTipo;
        this.itemId = itemId;
        this.cantidad = cantidad;
        this.fecha = Instant.now();
        this.motivo = motivo;
        this.referenciaId = referenciaId;
    }

    public Long getId() {
        return id;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public ItemTipo getItemTipo() {
        return itemTipo;
    }

    public Long getItemId() {
        return itemId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public Instant getFecha() {
        return fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public Long getReferenciaId() {
        return referenciaId;
    }
}
