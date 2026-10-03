package com.cerrajeria.sig.dashboard;

import org.springframework.transaction.annotation.Transactional;
import com.cerrajeria.sig.common.audit.AuditLogRepository;
import com.cerrajeria.sig.inventario.Producto;
import com.cerrajeria.sig.inventario.ProductoRepository;
import com.cerrajeria.sig.ordenes.EstadoOrden;
import com.cerrajeria.sig.ordenes.OrdenTrabajoRepository;
import com.cerrajeria.sig.domicilio.ServicioDomicilioRepository;
import com.cerrajeria.sig.ventas.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VentaRepository ventaRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final ServicioDomicilioRepository servicioDomicilioRepository;
    private final ProductoRepository productoRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public DashboardResponse obtener() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        var ventasHoy = ventaRepository.totalEntreFechas(hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX));
        var ventasMes = ventaRepository.totalEntreFechas(inicioMes.atStartOfDay(), hoy.atTime(LocalTime.MAX));

        long pendientes = ordenTrabajoRepository.countByEstadoNot(EstadoOrden.ENTREGADO);
        long listos = ordenTrabajoRepository.countByEstado(EstadoOrden.LISTO);

        long serviciosProximos = servicioDomicilioRepository
            .findByFechaBetweenOrderByFechaAscHoraAsc(hoy, hoy.plusDays(1)).size();

        var productosStockBajo = productoRepository.findStockBajo();
        var alertas = productosStockBajo.stream()
            .limit(5)
            .map(Producto::getNombre)
            .toList();

        // RF-09: ultimos movimientos, a partir del registro de auditoria (RF-47)
        var ultimosMovimientos = auditLogRepository.findTop10ByOrderByFechaHoraDesc().stream()
            .map(log -> new DashboardResponse.MovimientoReciente(
                descripcionAccion(log.getAccion(), log.getEntidad(), log.getEntidadId()),
                log.getUsuario().getNombre(),
                log.getFechaHora()
            ))
            .toList();

        return new DashboardResponse(
            ventasHoy, ventasMes, pendientes, listos,
            (long) serviciosProximos, (long) productosStockBajo.size(), alertas, ultimosMovimientos);
    }

    private String descripcionAccion(com.cerrajeria.sig.common.audit.AuditAccion accion, String entidad, Long entidadId) {
        String sujeto = entidad + " #" + entidadId;
        return switch (accion) {
            case APERTURA_CAJA -> "Apertura de caja";
            case CIERRE_CAJA -> "Cierre de caja";
            case AJUSTE_INVENTARIO -> "Ajuste de stock en " + sujeto;
            case CAMBIO_ESTADO -> "Cambio de estado en " + sujeto;
            case CREAR -> "Creación de " + sujeto;
            case EDITAR -> "Edición de " + sujeto;
            case ELIMINAR -> "Eliminación de " + sujeto;
            case ANULACION -> "Anulación de " + sujeto;
        };
    }
}
