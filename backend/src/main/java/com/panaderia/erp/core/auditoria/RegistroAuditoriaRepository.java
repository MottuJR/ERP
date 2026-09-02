package com.panaderia.erp.core.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    List<RegistroAuditoria> findByEntidadOrderByFechaDesc(String entidad);

    List<RegistroAuditoria> findByEntidadAndEntidadIdOrderByFechaDesc(String entidad, Long entidadId);

    List<RegistroAuditoria> findAllByOrderByFechaDesc();
}
