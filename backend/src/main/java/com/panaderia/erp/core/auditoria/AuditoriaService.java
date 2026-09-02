package com.panaderia.erp.core.auditoria;

import com.panaderia.erp.core.auditoria.dto.RegistroAuditoriaResponse;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registro genérico de auditoría para operaciones sensibles (altas/bajas, ajustes de stock,
 * cambios de precio, apertura/cierre de caja). No es un mecanismo sofisticado a propósito:
 * una tabla plana (usuarioId, entidad, entidadId, accion, fecha, detalle) alcanza para poder
 * responder "quién hizo qué y cuándo".
 */
@Service
public class AuditoriaService {

    private final RegistroAuditoriaRepository registroAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(RegistroAuditoriaRepository registroAuditoriaRepository,
                             UsuarioRepository usuarioRepository) {
        this.registroAuditoriaRepository = registroAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrar(String emailUsuario, String entidad, Long entidadId, AccionAuditoria accion,
                           String detalle) {
        Long usuarioId = usuarioRepository.findByEmail(emailUsuario)
                .map(Usuario::getId)
                .orElse(null);

        registroAuditoriaRepository.save(
                new RegistroAuditoria(usuarioId, entidad, entidadId, accion.name(), detalle));
    }

    public List<RegistroAuditoriaResponse> listar(String entidad, Long entidadId) {
        List<RegistroAuditoria> registros;

        if (entidad != null && entidadId != null) {
            registros = registroAuditoriaRepository.findByEntidadAndEntidadIdOrderByFechaDesc(entidad, entidadId);
        } else if (entidad != null) {
            registros = registroAuditoriaRepository.findByEntidadOrderByFechaDesc(entidad);
        } else {
            registros = registroAuditoriaRepository.findAllByOrderByFechaDesc();
        }

        Map<Long, String> nombresPorUsuarioId = usuarioRepository.findAll().stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNombre));

        return registros.stream()
                .map(r -> new RegistroAuditoriaResponse(
                        r.getId(), r.getUsuarioId(), nombresPorUsuarioId.get(r.getUsuarioId()),
                        r.getEntidad(), r.getEntidadId(), r.getAccion(), r.getFecha(), r.getDetalle()))
                .toList();
    }
}
