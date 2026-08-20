package com.panaderia.erp.caja;

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
@Table(name = "movimientos_caja")
public class MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "caja_id", nullable = false)
    private Long cajaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimientoCaja tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 255)
    private String concepto;

    @Column(nullable = false)
    private Instant fecha;

    protected MovimientoCaja() {
    }

    public MovimientoCaja(Long cajaId, TipoMovimientoCaja tipo, BigDecimal monto, String concepto) {
        this.cajaId = cajaId;
        this.tipo = tipo;
        this.monto = monto;
        this.concepto = concepto;
        this.fecha = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getCajaId() {
        return cajaId;
    }

    public TipoMovimientoCaja getTipo() {
        return tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getConcepto() {
        return concepto;
    }

    public Instant getFecha() {
        return fecha;
    }
}
