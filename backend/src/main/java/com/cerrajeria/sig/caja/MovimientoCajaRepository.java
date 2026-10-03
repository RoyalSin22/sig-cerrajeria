package com.cerrajeria.sig.caja;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {
    List<MovimientoCaja> findByCajaDiariaIdOrderByFechaHoraDesc(Long cajaDiariaId);
}
