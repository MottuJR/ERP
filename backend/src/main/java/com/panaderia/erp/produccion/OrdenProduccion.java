package com.panaderia.erp.produccion;

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
@Table(name = "ordenes_produccion")
public class OrdenProduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(nullable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrdenProduccion estado;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    protected OrdenProduccion() {
    }

    public OrdenProduccion(Long productoId, BigDecimal cantidad, Long usuarioId) {
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.usuarioId = usuarioId;
        this.fecha = Instant.now();
        this.estado = EstadoOrdenProduccion.CONFIRMADA;
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public Instant getFecha() {
        return fecha;
    }

    public EstadoOrdenProduccion getEstado() {
        return estado;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }
}
