package com.panaderia.erp.ventas;

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
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant fecha;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "caja_id")
    private Long cajaId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 20)
    private MedioPago medioPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVenta estado;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<DetalleVenta> detalles = new ArrayList<>();

    protected Venta() {
    }

    public Venta(Long clienteId, Long usuarioId, Long cajaId, MedioPago medioPago) {
        this.fecha = Instant.now();
        this.clienteId = clienteId;
        this.usuarioId = usuarioId;
        this.cajaId = cajaId;
        this.medioPago = medioPago;
        this.estado = EstadoVenta.CONFIRMADA;
        this.total = BigDecimal.ZERO;
    }

    public void agregarDetalle(Long productoId, BigDecimal cantidad, BigDecimal precioUnitario) {
        BigDecimal subtotal = precioUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
        detalles.add(new DetalleVenta(this, productoId, cantidad, precioUnitario, subtotal));
        total = total.add(subtotal);
    }

    public Long getId() {
        return id;
    }

    public Instant getFecha() {
        return fecha;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getCajaId() {
        return cajaId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public List<DetalleVenta> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }
}
