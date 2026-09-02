package com.panaderia.erp.clientes;

import com.panaderia.erp.core.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes")
public class Cliente extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    @Column(name = "tiene_cuenta_corriente", nullable = false)
    private boolean tieneCuentaCorriente;

    @Column(nullable = false)
    private boolean activo = true;

    protected Cliente() {
    }

    public Cliente(String nombre, String telefono, boolean tieneCuentaCorriente) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.tieneCuentaCorriente = tieneCuentaCorriente;
        this.activo = true;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public boolean isTieneCuentaCorriente() {
        return tieneCuentaCorriente;
    }

    public void setTieneCuentaCorriente(boolean tieneCuentaCorriente) {
        this.tieneCuentaCorriente = tieneCuentaCorriente;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
