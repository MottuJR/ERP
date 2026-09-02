package com.panaderia.erp.core.auditoria.dto;

import java.time.Instant;

public record RegistroAuditoriaResponse(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String entidad,
        Long entidadId,
        String accion,
        Instant fecha,
        String detalle
) {
}
