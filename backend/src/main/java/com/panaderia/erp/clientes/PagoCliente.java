package com.panaderia.erp.clientes;

import com.panaderia.erp.ventas.MedioPago;
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
@Table(name = "pagos_cliente")
public class PagoCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private Instant fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 20)
    private MedioPago medioPago;

    protected PagoCliente() {
    }

    public PagoCliente(Long clienteId, BigDecimal monto, MedioPago medioPago) {
        this.clienteId = clienteId;
        this.fecha = Instant.now();
        this.monto = monto;
        this.medioPago = medioPago;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Instant getFecha() {
        return fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }
}
