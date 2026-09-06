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
@Table(name = "cajas")
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_apertura", nullable = false)
    private Instant fechaApertura;

    @Column(name = "fecha_cierre")
    private Instant fechaCierre;

    @Column(name = "monto_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoInicial;

    @Column(name = "monto_final", precision = 12, scale = 2)
    private BigDecimal montoFinal;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCaja estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "comision_medio_pago", length = 20)
    private MedioPagoComision comisionMedioPago;

    @Column(name = "comision_monto", precision = 12, scale = 2)
    private BigDecimal comisionMonto;

    protected Caja() {
    }

    public Caja(BigDecimal montoInicial, Long usuarioId) {
        this.fechaApertura = Instant.now();
        this.montoInicial = montoInicial;
        this.usuarioId = usuarioId;
        this.estado = EstadoCaja.ABIERTA;
    }

    public Long getId() {
        return id;
    }

    public Instant getFechaApertura() {
        return fechaApertura;
    }

    public Instant getFechaCierre() {
        return fechaCierre;
    }

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public BigDecimal getMontoFinal() {
        return montoFinal;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public EstadoCaja getEstado() {
        return estado;
    }

    public MedioPagoComision getComisionMedioPago() {
        return comisionMedioPago;
    }

    public BigDecimal getComisionMonto() {
        return comisionMonto;
    }

    public void cerrar(BigDecimal montoFinal, MedioPagoComision comisionMedioPago, BigDecimal comisionMonto) {
        this.montoFinal = montoFinal;
        this.fechaCierre = Instant.now();
        this.estado = EstadoCaja.CERRADA;
        this.comisionMedioPago = comisionMedioPago;
        this.comisionMonto = comisionMonto;
    }
}
