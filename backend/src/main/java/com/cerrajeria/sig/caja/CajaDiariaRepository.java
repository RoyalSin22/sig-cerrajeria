package com.cerrajeria.sig.caja;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CajaDiariaRepository extends JpaRepository<CajaDiaria, Long> {
    Optional<CajaDiaria> findByUsuarioUsernameAndEstado(String username, EstadoCaja estado);
    List<CajaDiaria> findByUsuarioUsernameOrderByFechaAperturaDesc(String username);
    org.springframework.data.domain.Page<CajaDiaria> findByUsuarioUsernameOrderByFechaAperturaDesc(
        String username, org.springframework.data.domain.Pageable pageable);
}
