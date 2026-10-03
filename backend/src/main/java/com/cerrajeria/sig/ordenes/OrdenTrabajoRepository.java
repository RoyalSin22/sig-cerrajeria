package com.cerrajeria.sig.ordenes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {
    List<OrdenTrabajo> findByEstadoOrderByFechaIngresoDesc(EstadoOrden estado);
    List<OrdenTrabajo> findByClienteIdOrderByFechaIngresoDesc(Long clienteId);
    List<OrdenTrabajo> findAllByOrderByFechaIngresoDesc();
    long countByEstadoNot(EstadoOrden estado);
    long countByEstado(EstadoOrden estado);

    org.springframework.data.domain.Page<OrdenTrabajo> findByEstadoOrderByFechaIngresoDesc(
        EstadoOrden estado, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<OrdenTrabajo> findAllByOrderByFechaIngresoDesc(
        org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT nextval('numero_orden_seq')", nativeQuery = true)
    Long siguienteNumeroSecuencia();
}
