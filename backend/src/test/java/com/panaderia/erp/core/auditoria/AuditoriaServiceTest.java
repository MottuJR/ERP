package com.panaderia.erp.core.auditoria;

import com.panaderia.erp.core.auditoria.dto.RegistroAuditoriaResponse;
import com.panaderia.erp.core.usuario.Rol;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private RegistroAuditoriaRepository registroAuditoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuditoriaService auditoriaService;

    private Usuario usuario(long id, String email, String nombre) {
        Usuario usuario = new Usuario(nombre, email, "hash", Rol.ENCARGADO);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    @Test
    void registrarResuelveElUsuarioIdAPartirDelEmailYGuardaElRegistro() {
        when(usuarioRepository.findByEmail("encargada@panaderia.local"))
                .thenReturn(Optional.of(usuario(7L, "encargada@panaderia.local", "Encargada")));

        auditoriaService.registrar("encargada@panaderia.local", "Producto", 3L, AccionAuditoria.CREAR, "Alta de Pan");

        ArgumentCaptor<RegistroAuditoria> captor = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(registroAuditoriaRepository).save(captor.capture());

        RegistroAuditoria guardado = captor.getValue();
        assertThat(guardado.getUsuarioId()).isEqualTo(7L);
        assertThat(guardado.getEntidad()).isEqualTo("Producto");
        assertThat(guardado.getEntidadId()).isEqualTo(3L);
        assertThat(guardado.getAccion()).isEqualTo("CREAR");
        assertThat(guardado.getDetalle()).isEqualTo("Alta de Pan");
    }

    @Test
    void siElEmailNoCorrespondeANingunUsuarioElRegistroQuedaSinUsuarioIdPeroSeGuardaIgual() {
        when(usuarioRepository.findByEmail("fantasma@panaderia.local")).thenReturn(Optional.empty());

        auditoriaService.registrar("fantasma@panaderia.local", "Caja", 1L, AccionAuditoria.ABRIR_CAJA, "Apertura");

        ArgumentCaptor<RegistroAuditoria> captor = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(registroAuditoriaRepository).save(captor.capture());

        assertThat(captor.getValue().getUsuarioId()).isNull();
    }

    @Test
    void listarFiltraPorEntidadYEntidadIdYResuelveElNombreDelUsuario() {
        Usuario usuario = usuario(7L, "dueno@panaderia.local", "Administradora");
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        RegistroAuditoria registro = new RegistroAuditoria(7L, "Producto", 3L, "CREAR", "Alta de Pan");
        when(registroAuditoriaRepository.findByEntidadAndEntidadIdOrderByFechaDesc("Producto", 3L))
                .thenReturn(List.of(registro));

        List<RegistroAuditoriaResponse> resultado = auditoriaService.listar("Producto", 3L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).usuarioNombre()).isEqualTo("Administradora");
        assertThat(resultado.get(0).accion()).isEqualTo("CREAR");
    }

    @Test
    void listarSinFiltrosTraeTodo() {
        when(usuarioRepository.findAll()).thenReturn(List.of());
        when(registroAuditoriaRepository.findAllByOrderByFechaDesc()).thenReturn(List.of());

        List<RegistroAuditoriaResponse> resultado = auditoriaService.listar(null, null);

        assertThat(resultado).isEmpty();
        verify(registroAuditoriaRepository, org.mockito.Mockito.never())
                .findByEntidadOrderByFechaDesc(any());
    }
}
