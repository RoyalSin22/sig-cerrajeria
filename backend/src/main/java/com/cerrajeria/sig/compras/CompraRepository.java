package com.cerrajeria.sig.compras;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByProductoIdOrderByFechaDesc(Long productoId);
    Page<Compra> findAllByOrderByFechaDesc(Pageable pageable);
}
