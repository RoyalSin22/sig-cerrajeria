package com.cerrajeria.sig.caja;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CajaDiariaRepository extends JpaRepository<CajaDiaria, Long> {
    Optional<CajaDiaria> findByUsuarioUsernameAndEstado(String username, EstadoCaja estado);
    List<CajaDiaria> findByUsuarioUsernameOrderByFechaAperturaDesc(String username);
    Page<CajaDiaria> findByUsuarioUsernameOrderByFechaAperturaDesc(
        String username, Pageable pageable);
}
