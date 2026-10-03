package com.cerrajeria.sig.inventario;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    // El mapper lee usuario.nombre fuera de transaccion (open-in-view: false):
    // se carga en la misma consulta para evitar LazyInitializationException.
    @EntityGraph(attributePaths = "usuario")
    List<MovimientoInventario> findByProductoIdOrderByFechaHoraDesc(Long productoId);
}
