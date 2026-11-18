package com.cerrajeria.sig.ordenes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {
    List<OrdenTrabajo> findByEstadoOrderByFechaIngresoDesc(EstadoOrden estado);
    List<OrdenTrabajo> findByClienteIdOrderByFechaIngresoDesc(Long clienteId);
    List<OrdenTrabajo> findAllByOrderByFechaIngresoDesc();
    /** Ordenes abiertas: las que no estan ENTREGADO ni CANCELADO. */
    long countByEstadoNotIn(Collection<EstadoOrden> estados);
    long countByEstado(EstadoOrden estado);
    /** RF-43: ordenes recibidas y entregadas en un rango. */
    long countByFechaIngresoBetween(LocalDateTime desde, LocalDateTime hasta);
    long countByEstadoAndFechaEntregaBetween(EstadoOrden estado, LocalDateTime desde, LocalDateTime hasta);

    Page<OrdenTrabajo> findByEstadoOrderByFechaIngresoDesc(
        EstadoOrden estado, Pageable pageable);
    Page<OrdenTrabajo> findAllByOrderByFechaIngresoDesc(
        Pageable pageable);

    @Query(value = "SELECT nextval('numero_orden_seq')", nativeQuery = true)
    Long siguienteNumeroSecuencia();
}
