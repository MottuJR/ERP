package com.panaderia.erp.core.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "registro_auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 60)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(nullable = false, length = 40)
    private String accion;

    @Column(nullable = false)
    private Instant fecha;

    @Column(length = 500)
    private String detalle;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(Long usuarioId, String entidad, Long entidadId, String accion, String detalle) {
        this.usuarioId = usuarioId;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.accion = accion;
        this.fecha = Instant.now();
        this.detalle = detalle;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getEntidad() {
        return entidad;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public String getAccion() {
        return accion;
    }

    public Instant getFecha() {
        return fecha;
    }

    public String getDetalle() {
        return detalle;
    }
}
