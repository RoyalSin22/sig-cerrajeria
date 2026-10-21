package com.cerrajeria.sig.caja;

import com.cerrajeria.sig.common.MetodoPago;
import com.cerrajeria.sig.caja.dto.AperturaCajaRequest;
import com.cerrajeria.sig.caja.dto.CajaDiariaResponse;
import com.cerrajeria.sig.caja.dto.CierreCajaRequest;
import com.cerrajeria.sig.caja.dto.CierreCajaResponse;
import com.cerrajeria.sig.caja.dto.MovimientoCajaRequest;
import com.cerrajeria.sig.caja.dto.MovimientoCajaResponse;
import com.cerrajeria.sig.common.audit.AuditAccion;
import com.cerrajeria.sig.common.audit.AuditLogService;
import com.cerrajeria.sig.common.audit.DetalleAuditoria;
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
    private final jakarta.persistence.EntityManager entityManager;

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

        auditLogService.registrar(usuario, AuditAccion.APERTURA_CAJA, "CajaDiaria", caja.getId(),
            DetalleAuditoria.de("Apertura de caja").cambio("montoApertura", null, caja.getMontoApertura()));
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

        bloquear(caja);
        BigDecimal efectivoAntes = caja.efectivoEsperado();
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
        movimiento = movimientoCajaRepository.save(movimiento);
        auditLogService.registrar(movimiento.getUsuario(), AuditAccion.MOVIMIENTO_CAJA, "CajaDiaria", caja.getId(),
            DetalleAuditoria.de((tipo == TipoMovimientoCaja.INGRESO ? "Ingreso" : "Egreso") + " manual de S/ "
                    + request.monto().setScale(2, java.math.RoundingMode.HALF_UP))
                .motivo(request.motivo())
                .cambio("efectivoEsperado", efectivoAntes, caja.efectivoEsperado()));
        return cajaMapper.toResponse(movimiento);
    }

    /**
     * Usado internamente por VentaService/OrdenTrabajoService al registrar cobros.
     * El metodo decide si el dinero entra al cajon (EFECTIVO) o no (Yape, Plin, tarjeta).
     */
    @Transactional
    public void sumarIngresoACajaActiva(CajaDiaria caja, BigDecimal monto, MetodoPago metodo) {
        bloquear(caja);
        caja.sumarIngreso(monto, metodo);
        cajaDiariaRepository.save(caja);
    }

    /** Egreso con el metodo con que sale el dinero: EFECTIVO, o el del cobro que se devuelve. */
    @Transactional
    public void sumarEgresoACajaActiva(CajaDiaria caja, BigDecimal monto, MetodoPago metodo) {
        bloquear(caja);
        caja.sumarEgreso(monto, metodo);
        cajaDiariaRepository.save(caja);
    }

    @Transactional
    public CierreCajaResponse cerrar(CierreCajaRequest request) {
        CajaDiaria caja = obtenerCajaAbiertaEntidad();
        bloquear(caja); // un cobro en curso termina antes de calcular el esperado
        // Se cuenta el cajon: Yape, Plin y tarjeta no estan ahi y no entran al cuadre.
        BigDecimal esperado = caja.efectivoEsperado();
        BigDecimal diferencia = request.montoContado().subtract(esperado);
        String observacion = request.observacion() == null ? null : request.observacion().trim();
        if (diferencia.signum() != 0 && (observacion == null || observacion.isEmpty())) {
            throw new IllegalArgumentException("El cajón no cuadra (" + (diferencia.signum() > 0 ? "sobran" : "faltan")
                + " S/ " + diferencia.abs().setScale(2, java.math.RoundingMode.HALF_UP)
                + "): escribe qué pasó antes de cerrar");
        }

        caja.setMontoCierre(request.montoContado());
        caja.setDiferenciaCierre(diferencia);
        caja.setObservacionCierre(observacion == null || observacion.isEmpty() ? null : observacion);
        caja.setFechaCierre(LocalDateTime.now());
        caja.setEstado(EstadoCaja.CERRADA);
        cajaDiariaRepository.save(caja);

        auditLogService.registrar(caja.getUsuario(), AuditAccion.CIERRE_CAJA, "CajaDiaria", caja.getId(),
            DetalleAuditoria.de(diferencia.signum() == 0 ? "Cierre de caja: cuadra"
                    : "Cierre de caja con diferencia de S/ " + diferencia.setScale(2, java.math.RoundingMode.HALF_UP))
                .motivo(caja.getObservacionCierre())
                .cambio("efectivoEsperado", null, esperado)
                .cambio("efectivoContado", null, request.montoContado()));
        return new CierreCajaResponse(cajaMapper.toResponse(caja), request.montoContado(), diferencia);
    }

    /**
     * Bloquea la fila de la caja (SELECT ... FOR UPDATE) hasta el fin de la
     * transaccion y la relee: dos cobros simultaneos de la misma caja (PC y
     * celular) no pisan sus totales, y un cobro que llega justo despues del
     * cierre se rechaza. El flush previo guarda lo que esta transaccion ya
     * sumo (ej. las lineas de un pago mixto) para que el refresh no lo pierda.
     */
    private void bloquear(CajaDiaria caja) {
        entityManager.flush();
        entityManager.refresh(caja, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new CajaNoAbiertaException("La caja ya se cerró. Abre una nueva para continuar.");
        }
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
