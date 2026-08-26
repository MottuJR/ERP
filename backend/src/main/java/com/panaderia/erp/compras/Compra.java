package com.panaderia.erp.compras;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "compras")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(nullable = false)
    private Instant fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCompra estado;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<DetalleCompra> detalles = new ArrayList<>();

    protected Compra() {
    }

    public Compra(Long proveedorId) {
        this.proveedorId = proveedorId;
        this.fecha = Instant.now();
        this.estado = EstadoCompra.CONFIRMADA;
        this.total = BigDecimal.ZERO;
    }

    public void agregarDetalle(Long insumoId, BigDecimal cantidad, BigDecimal costoUnitario) {
        BigDecimal subtotal = costoUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
        detalles.add(new DetalleCompra(this, insumoId, cantidad, costoUnitario, subtotal));
        total = total.add(subtotal);
    }

    public Long getId() {
        return id;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public Instant getFecha() {
        return fecha;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public EstadoCompra getEstado() {
        return estado;
    }

    public List<DetalleCompra> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }
}
