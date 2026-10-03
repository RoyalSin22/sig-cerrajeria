package com.cerrajeria.sig.caja;

import com.cerrajeria.sig.caja.dto.AperturaCajaRequest;
import com.cerrajeria.sig.caja.dto.CajaDiariaResponse;
import com.cerrajeria.sig.caja.dto.CierreCajaRequest;
import com.cerrajeria.sig.caja.dto.CierreCajaResponse;
import com.cerrajeria.sig.caja.dto.MovimientoCajaRequest;
import com.cerrajeria.sig.caja.dto.MovimientoCajaResponse;
import com.cerrajeria.sig.common.audit.AuditAccion;
import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.common.exception.CajaNoAbiertaException;
import com.cerrajeria.sig.common.exception.RecursoNoEncontradoException;
import com.cerrajeria.sig.usuarios.Usuario;
import com.cerrajeria.sig.usuarios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Nucleo de la regla de negocio 1 (Fase 1): ninguna operacion monetaria puede
 * procesarse sin una caja diaria ABIERTA para el usuario en sesion.
 * Otros modulos (Ventas, Ordenes, Compras) llaman a
 * {@link #obtenerCajaAbiertaEntidad()} antes de procesar cualquier cobro.
 */
@Service
@RequiredArgsConstructor
public class CajaService {

    private final CajaDiariaRepository cajaDiariaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaMapper cajaMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public CajaDiariaResponse abrir(AperturaCajaRequest request) {
        String username = usernameActual();
        cajaDiariaRepository.findByUsuarioUsernameAndEstado(username, EstadoCaja.ABIERTA).ifPresent(c -> {
            throw new IllegalArgumentException("Ya tienes una caja abierta (ID " + c.getId() + ")");
        });

        Usuario usuario = usuarioActual();
        CajaDiaria caja = new CajaDiaria();
        caja.setUsuario(usuario);
        caja.setMontoApertura(request.montoApertura());
        caja.setEstado(EstadoCaja.ABIERTA);
        caja = cajaDiariaRepository.save(caja);

        auditLogService.registrar(usuario, AuditAccion.APERTURA_CAJA, "CajaDiaria", caja.getId());
        return cajaMapper.toResponse(caja);
    }

    @Transactional(readOnly = true)
    public CajaDiariaResponse obtenerCajaAbierta() {
        return cajaMapper.toResponse(obtenerCajaAbiertaEntidad());
    }

    /**
     * Punto de entrada usado por otros servicios (VentaService,
     * OrdenTrabajoService, CompraService) para validar la regla de negocio 1
     * antes de procesar cualquier transaccion monetaria.
     */
    public CajaDiaria obtenerCajaAbiertaEntidad() {
        return cajaDiariaRepository.findByUsuarioUsernameAndEstado(usernameActual(), EstadoCaja.ABIERTA)
            .orElseThrow(() -> new CajaNoAbiertaException(
                "No tienes una caja diaria abierta. Debes abrir caja antes de continuar."));
    }

    @Transactional
    public MovimientoCajaResponse registrarMovimientoManual(MovimientoCajaRequest request) {
        CajaDiaria caja = obtenerCajaAbiertaEntidad();
        TipoMovimientoCaja tipo;
        try {
            tipo = TipoMovimientoCaja.valueOf(request.tipo().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Tipo de movimiento inválido, use INGRESO o EGRESO");
        }

        if (tipo == TipoMovimientoCaja.INGRESO) {
            caja.sumarIngreso(request.monto());
        } else {
            caja.sumarEgreso(request.monto());
        }
        cajaDiariaRepository.save(caja);

        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setCajaDiaria(caja);
        movimiento.setTipo(tipo);
        movimiento.setMonto(request.monto());
        movimiento.setMotivo(request.motivo());
        movimiento.setUsuario(usuarioActual());
        return cajaMapper.toResponse(movimientoCajaRepository.save(movimiento));
    }

    /** Usado internamente por VentaService/OrdenTrabajoService al registrar cobros. */
    @Transactional
    public void sumarIngresoACajaActiva(CajaDiaria caja, BigDecimal monto) {
        caja.sumarIngreso(monto);
        cajaDiariaRepository.save(caja);
    }

    @Transactional
    public void sumarEgresoACajaActiva(CajaDiaria caja, BigDecimal monto) {
        caja.sumarEgreso(monto);
        cajaDiariaRepository.save(caja);
    }

    @Transactional
    public CierreCajaResponse cerrar(CierreCajaRequest request) {
        CajaDiaria caja = obtenerCajaAbiertaEntidad();
        BigDecimal saldoEsperado = caja.saldoEsperado();
        BigDecimal diferencia = request.montoContado().subtract(saldoEsperado);

        caja.setMontoCierre(request.montoContado());
        caja.setFechaCierre(LocalDateTime.now());
        caja.setEstado(EstadoCaja.CERRADA);
        cajaDiariaRepository.save(caja);

        auditLogService.registrar(caja.getUsuario(), AuditAccion.CIERRE_CAJA, "CajaDiaria", caja.getId());
        return new CierreCajaResponse(cajaMapper.toResponse(caja), request.montoContado(), diferencia);
    }

    /** RF-37: historial de todas las sesiones de caja del usuario en sesion. */
    @Transactional(readOnly = true)
    public List<CajaDiariaResponse> historial() {
        return cajaDiariaRepository.findByUsuarioUsernameOrderByFechaAperturaDesc(usernameActual())
            .stream().map(cajaMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CajaDiariaResponse> historialPaginado(
            org.springframework.data.domain.Pageable pageable) {
        return cajaDiariaRepository
            .findByUsuarioUsernameOrderByFechaAperturaDesc(usernameActual(), pageable)
            .map(cajaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<MovimientoCajaResponse> historialMovimientos(Long cajaId) {
        return cajaMapper.toResponseList(movimientoCajaRepository.findByCajaDiariaIdOrderByFechaHoraDesc(cajaId));
    }

    private String usernameActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Usuario usuarioActual() {
        return usuarioRepository.findByUsername(usernameActual())
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
