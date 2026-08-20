package com.panaderia.erp.caja;

import com.panaderia.erp.core.exception.ConflictoException;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CajaService {

    private final CajaRepository cajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final UsuarioRepository usuarioRepository;

    public CajaService(CajaRepository cajaRepository,
                        MovimientoCajaRepository movimientoCajaRepository,
                        UsuarioRepository usuarioRepository) {
        this.cajaRepository = cajaRepository;
        this.movimientoCajaRepository = movimientoCajaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Caja obtenerPorId(Long id) {
        return cajaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Caja no encontrada: " + id));
    }

    public Optional<Caja> obtenerCajaAbierta() {
        return cajaRepository.findFirstByEstado(EstadoCaja.ABIERTA);
    }

    public List<MovimientoCaja> listarMovimientos(Long cajaId) {
        return movimientoCajaRepository.findByCajaIdOrderByFechaDesc(cajaId);
    }

    @Transactional
    public Caja abrirTurno(BigDecimal montoInicial, String emailUsuario) {
        if (obtenerCajaAbierta().isPresent()) {
            throw new ConflictoException("Ya hay una caja abierta. Hay que cerrarla antes de abrir otra.");
        }

        Usuario usuario = obtenerUsuario(emailUsuario);
        return cajaRepository.save(new Caja(montoInicial, usuario.getId()));
    }

    @Transactional
    public Caja cerrarTurno(Long cajaId, BigDecimal montoFinal) {
        Caja caja = obtenerPorId(cajaId);

        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new ValidacionNegocioException("La caja ya está cerrada");
        }

        caja.cerrar(montoFinal);
        return caja;
    }

    @Transactional
    public MovimientoCaja registrarMovimiento(Long cajaId, TipoMovimientoCaja tipo, BigDecimal monto, String concepto) {
        Caja caja = obtenerPorId(cajaId);

        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new ValidacionNegocioException("No se pueden registrar movimientos en una caja cerrada");
        }

        return movimientoCajaRepository.save(new MovimientoCaja(cajaId, tipo, monto, concepto));
    }

    private Usuario obtenerUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado"));
    }
}
