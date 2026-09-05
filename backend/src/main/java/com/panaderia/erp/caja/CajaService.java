package com.panaderia.erp.caja;

import com.panaderia.erp.caja.dto.CajaHistorialResponse;
import com.panaderia.erp.caja.dto.CajaResumenResponse;
import com.panaderia.erp.caja.dto.PagoResumenCajaResponse;
import com.panaderia.erp.caja.dto.VentaPorMedioPagoDTO;
import com.panaderia.erp.caja.dto.VentaResumenCajaResponse;
import com.panaderia.erp.clientes.ClienteService;
import com.panaderia.erp.clientes.CuentaCorrienteService;
import com.panaderia.erp.clientes.PagoCliente;
import com.panaderia.erp.clientes.dto.PagoPorMedioPagoResumen;
import com.panaderia.erp.core.auditoria.AccionAuditoria;
import com.panaderia.erp.core.auditoria.AuditoriaService;
import com.panaderia.erp.core.exception.ConflictoException;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.exception.ValidacionNegocioException;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import com.panaderia.erp.ventas.MedioPago;
import com.panaderia.erp.ventas.Venta;
import com.panaderia.erp.ventas.VentaService;
import com.panaderia.erp.ventas.dto.VentaPorMedioPagoResumen;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CajaService {

    private static final String ENTIDAD = "Caja";

    private final CajaRepository cajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final VentaService ventaService;
    private final ClienteService clienteService;
    private final CuentaCorrienteService cuentaCorrienteService;

    public CajaService(CajaRepository cajaRepository,
                        MovimientoCajaRepository movimientoCajaRepository,
                        UsuarioRepository usuarioRepository,
                        AuditoriaService auditoriaService,
                        VentaService ventaService,
                        ClienteService clienteService,
                        CuentaCorrienteService cuentaCorrienteService) {
        this.cajaRepository = cajaRepository;
        this.movimientoCajaRepository = movimientoCajaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
        this.ventaService = ventaService;
        this.clienteService = clienteService;
        this.cuentaCorrienteService = cuentaCorrienteService;
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

    /**
     * Historial completo de turnos (abiertos y cerrados), del más reciente al más viejo.
     * Solo lo consulta el módulo de caja para DUENO/ENCARGADO — ver historial de turnos pasados.
     */
    public List<CajaHistorialResponse> listarHistorial() {
        return cajaRepository.findAllByOrderByFechaAperturaDesc().stream()
                .map(caja -> CajaHistorialResponse.from(caja, nombreUsuario(caja.getUsuarioId())))
                .toList();
    }

    private String nombreUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId).map(Usuario::getNombre).orElse("—");
    }

    /**
     * Resumen de un turno puntual: ventas y cobros de cuenta corriente agrupados por medio de
     * pago, ingresos/egresos manuales, y la diferencia entre el efectivo esperado (inicial +
     * ventas en efectivo + cobros en efectivo + ingresos - egresos) y el monto final contado al
     * cierre. Un cobro de cuenta corriente en efectivo entra físicamente en el cajón igual que
     * una venta en efectivo, así que tiene que contar para el efectivo esperado.
     */
    public CajaResumenResponse obtenerResumen(Long cajaId) {
        Caja caja = obtenerPorId(cajaId);
        String usuarioNombre = nombreUsuario(caja.getUsuarioId());

        List<VentaPorMedioPagoResumen> ventasPorMedio = ventaService.resumenPorMedioPago(cajaId);
        List<PagoPorMedioPagoResumen> cobrosPorMedio = cuentaCorrienteService.resumenPorMedioPago(cajaId);
        List<MovimientoCaja> movimientos = movimientoCajaRepository.findByCajaIdOrderByFechaDesc(cajaId);

        BigDecimal totalIngresos = sumarPorTipo(movimientos, TipoMovimientoCaja.INGRESO);
        BigDecimal totalEgresos = sumarPorTipo(movimientos, TipoMovimientoCaja.EGRESO);

        BigDecimal totalEfectivoVentas = ventasPorMedio.stream()
                .filter(v -> v.medioPago() == MedioPago.EFECTIVO)
                .map(VentaPorMedioPagoResumen::total)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        BigDecimal totalEfectivoCobros = cobrosPorMedio.stream()
                .filter(c -> c.medioPago() == MedioPago.EFECTIVO)
                .map(PagoPorMedioPagoResumen::total)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        BigDecimal totalVentas = ventasPorMedio.stream().map(VentaPorMedioPagoResumen::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCobros = cobrosPorMedio.stream().map(PagoPorMedioPagoResumen::total).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal efectivoEsperado = caja.getMontoInicial()
                .add(totalEfectivoVentas).add(totalEfectivoCobros).add(totalIngresos).subtract(totalEgresos);
        BigDecimal diferencia = caja.getMontoFinal() != null ? caja.getMontoFinal().subtract(efectivoEsperado) : null;

        List<VentaResumenCajaResponse> ventas = ventaService.listarPorCaja(cajaId).stream()
                .map(this::aVentaResumenCaja)
                .toList();
        List<PagoResumenCajaResponse> cobros = cuentaCorrienteService.listarPorCaja(cajaId).stream()
                .map(this::aPagoResumenCaja)
                .toList();

        return new CajaResumenResponse(
                caja.getId(), caja.getFechaApertura(), caja.getFechaCierre(),
                caja.getMontoInicial(), caja.getMontoFinal(), caja.getUsuarioId(), usuarioNombre, caja.getEstado(),
                ventasPorMedio.stream().map(VentaPorMedioPagoDTO::from).toList(),
                totalVentas, totalIngresos, totalEgresos, efectivoEsperado, diferencia, ventas,
                cobrosPorMedio.stream().map(VentaPorMedioPagoDTO::fromPago).toList(), totalCobros, cobros);
    }

    private VentaResumenCajaResponse aVentaResumenCaja(Venta venta) {
        String clienteNombre = venta.getClienteId() != null
                ? clienteService.obtenerPorId(venta.getClienteId()).getNombre()
                : null;

        return new VentaResumenCajaResponse(
                venta.getId(), venta.getFecha(), venta.getMedioPago(), venta.getTotal(),
                nombreUsuario(venta.getUsuarioId()), clienteNombre);
    }

    private PagoResumenCajaResponse aPagoResumenCaja(PagoCliente pago) {
        String clienteNombre = clienteService.obtenerPorId(pago.getClienteId()).getNombre();

        return new PagoResumenCajaResponse(
                pago.getId(), pago.getFecha(), pago.getMedioPago(), pago.getMonto(),
                nombreUsuario(pago.getUsuarioId()), clienteNombre);
    }

    private BigDecimal sumarPorTipo(List<MovimientoCaja> movimientos, TipoMovimientoCaja tipo) {
        return movimientos.stream()
                .filter(m -> m.getTipo() == tipo)
                .map(MovimientoCaja::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Caja abrirTurno(BigDecimal montoInicial, String emailUsuario) {
        if (obtenerCajaAbierta().isPresent()) {
            throw new ConflictoException("Ya hay una caja abierta. Hay que cerrarla antes de abrir otra.");
        }

        Usuario usuario = obtenerUsuario(emailUsuario);
        Caja caja = cajaRepository.save(new Caja(montoInicial, usuario.getId()));

        auditoriaService.registrar(emailUsuario, ENTIDAD, caja.getId(), AccionAuditoria.ABRIR_CAJA,
                "Apertura con monto inicial %s".formatted(montoInicial));

        return caja;
    }

    @Transactional
    public Caja cerrarTurno(Long cajaId, BigDecimal montoFinal, String emailUsuario) {
        Caja caja = obtenerPorId(cajaId);

        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new ValidacionNegocioException("La caja ya está cerrada");
        }

        caja.cerrar(montoFinal);

        auditoriaService.registrar(emailUsuario, ENTIDAD, caja.getId(), AccionAuditoria.CERRAR_CAJA,
                "Cierre con monto final %s".formatted(montoFinal));

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
