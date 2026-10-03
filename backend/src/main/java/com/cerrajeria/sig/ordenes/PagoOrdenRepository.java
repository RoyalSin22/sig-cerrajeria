package com.cerrajeria.sig.ordenes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoOrdenRepository extends JpaRepository<PagoOrden, Long> {
    List<PagoOrden> findByOrdenTrabajoIdOrderByFechaHoraDesc(Long ordenTrabajoId);
}
