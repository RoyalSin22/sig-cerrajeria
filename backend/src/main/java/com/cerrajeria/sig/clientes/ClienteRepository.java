package com.cerrajeria.sig.clientes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    // findAll(Pageable) ya viene heredado de JpaRepository: se usa para "listar todos" paginado.

    /** Busqueda rapida (RF-18), aprovechando los indices GIN + pg_trgm de Fase 6. */
    @Query(value = """
        SELECT * FROM clientes
        WHERE nombre ILIKE CONCAT('%', :term, '%') OR telefono ILIKE CONCAT('%', :term, '%')
        ORDER BY nombre
        LIMIT 20
        """, nativeQuery = true)
    List<Cliente> buscar(@Param("term") String term);
}
