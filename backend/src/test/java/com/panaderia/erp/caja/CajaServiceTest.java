package com.panaderia.erp.caja;

import com.panaderia.erp.comisiones.ComisionesService;
import com.panaderia.erp.core.auditoria.AccionAuditoria;
import com.panaderia.erp.core.auditoria.AuditoriaService;
import com.panaderia.erp.core.exception.ConflictoException;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import com.panaderia.erp.core.usuario.Rol;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CajaServiceTest {

    private static final String EMAIL = "vendedora@panaderia.local";

    @Mock
    private CajaRepository cajaRepository;

    @Mock
    private MovimientoCajaRepository movimientoCajaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private ComisionesService comisionesService;

    @InjectMocks
    private CajaService cajaService;

    private Usuario usuario() {
        Usuario usuario = new Usuario("Vendedora", EMAIL, "hash", Rol.VENDEDOR);
        ReflectionTestUtils.setField(usuario, "id", 1L);
        return usuario;
    }

    private Caja cajaAbierta(long id) {
        Caja caja = new Caja(new BigDecimal("5000.00"), 1L);
        ReflectionTestUtils.setField(caja, "id", id);
        return caja;
    }

    @Test
    void abrirTurnoGuardaLaCajaYRegistraAuditoria() {
        when(cajaRepository.findFirstByEstado(EstadoCaja.ABIERTA)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario()));
        when(cajaRepository.save(any(Caja.class))).thenAnswer(inv -> {
            Caja caja = inv.getArgument(0);
            ReflectionTestUtils.setField(caja, "id", 5L);
            return caja;
        });

        Caja caja = cajaService.abrirTurno(new BigDecimal("5000.00"), EMAIL);

        assertThat(caja.getId()).isEqualTo(5L);
        verify(auditoriaService).registrar(eq(EMAIL), eq("Caja"), eq(5L), eq(AccionAuditoria.ABRIR_CAJA),
                contains("5000.00"));
    }

    @Test
    void noSePuedeAbrirUnaCajaSiYaHayUnaAbierta() {
        when(cajaRepository.findFirstByEstado(EstadoCaja.ABIERTA)).thenReturn(Optional.of(cajaAbierta(1L)));

        assertThatThrownBy(() -> cajaService.abrirTurno(new BigDecimal("5000.00"), EMAIL))
                .isInstanceOf(ConflictoException.class);

        verify(cajaRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void cerrarTurnoCierraLaCajaYRegistraAuditoriaConElMontoFinal() {
        Caja caja = cajaAbierta(5L);
        when(cajaRepository.findById(5L)).thenReturn(Optional.of(caja));

        Caja cerrada = cajaService.cerrarTurno(5L, new BigDecimal("5800.00"), null, EMAIL);

        assertThat(cerrada.getEstado()).isEqualTo(EstadoCaja.CERRADA);
        assertThat(cerrada.getMontoFinal()).isEqualByComparingTo("5800.00");
        assertThat(cerrada.getComisionMedioPago()).isNull();
        assertThat(cerrada.getComisionMonto()).isNull();
        verify(auditoriaService).registrar(eq(EMAIL), eq("Caja"), eq(5L), eq(AccionAuditoria.CERRAR_CAJA),
                contains("5800.00"));
        verify(comisionesService, never()).comisionTotalDeTurno(any());
    }

    @Test
    void cerrarTurnoConComisionEnEfectivoCalculaYGuardaElMontoDeComision() {
        Caja caja = cajaAbierta(5L);
        when(cajaRepository.findById(5L)).thenReturn(Optional.of(caja));
        when(comisionesService.comisionTotalDeTurno(5L)).thenReturn(new BigDecimal("300.00"));

        Caja cerrada = cajaService.cerrarTurno(5L, new BigDecimal("5800.00"), MedioPagoComision.EFECTIVO, EMAIL);

        assertThat(cerrada.getComisionMedioPago()).isEqualTo(MedioPagoComision.EFECTIVO);
        assertThat(cerrada.getComisionMonto()).isEqualByComparingTo("300.00");
        verify(auditoriaService).registrar(eq(EMAIL), eq("Caja"), eq(5L), eq(AccionAuditoria.CERRAR_CAJA),
                contains("300.00"));
    }

    @Test
    void noSePuedeCerrarUnaCajaQueYaEstaCerrada() {
        Caja caja = cajaAbierta(5L);
        caja.cerrar(new BigDecimal("5800.00"), null, null);
        when(cajaRepository.findById(5L)).thenReturn(Optional.of(caja));

        assertThatThrownBy(() -> cajaService.cerrarTurno(5L, new BigDecimal("6000.00"), null, EMAIL))
                .isInstanceOf(ValidacionNegocioException.class);

        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void noSePuedenRegistrarMovimientosEnUnaCajaCerrada() {
        Caja caja = cajaAbierta(5L);
        caja.cerrar(new BigDecimal("5800.00"), null, null);
        when(cajaRepository.findById(5L)).thenReturn(Optional.of(caja));

        assertThatThrownBy(() -> cajaService.registrarMovimiento(5L, TipoMovimientoCaja.INGRESO,
                new BigDecimal("100.00"), "Vuelto"))
                .isInstanceOf(ValidacionNegocioException.class);

        verify(movimientoCajaRepository, never()).save(any());
    }
}
