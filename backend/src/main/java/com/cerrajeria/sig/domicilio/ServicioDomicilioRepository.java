package com.cerrajeria.sig.domicilio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ServicioDomicilioRepository extends JpaRepository<ServicioDomicilio, Long> {
    List<ServicioDomicilio> findByFechaBetweenOrderByFechaAscHoraAsc(LocalDate desde, LocalDate hasta);
    List<ServicioDomicilio> findByClienteIdOrderByFechaDesc(Long clienteId);
}
