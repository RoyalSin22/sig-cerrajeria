package com.cerrajeria.sig.inventario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

    /** Codigos de la lista que ya estan en uso, incluso por productos dados de baja (la columna es UNIQUE). */
    @Query("SELECT p.codigo FROM Producto p WHERE p.codigo IN :codigos")
    List<String> findCodigosExistentes(@Param("codigos") java.util.Collection<String> codigos);

    /** Listado paginado del catálogo completo (sin buscar), usado para explorar por categoría. */
    Page<Producto> findByActivoTrueOrderByNombreAsc(Pageable pageable);

    Page<Producto> findByActivoTrueAndTipoProductoOrderByNombreAsc(TipoProducto tipoProducto, Pageable pageable);

    /** Todo el catalogo activo, para descargarlo como planilla. */
    List<Producto> findByActivoTrueOrderByTipoProductoAscNombreAsc();

    List<Producto> findByCodigoIn(java.util.Collection<String> codigos);

    /**
     * Busqueda instantanea (RF-46), aprovechando los indices GIN + pg_trgm
     * creados en la Fase 6 sobre nombre y codigo. Se mantiene acotada a 20
     * resultados a proposito: es un autocompletado, no un listado paginado.
     */
    @Query(value = """
        SELECT * FROM productos
        WHERE activo = true
          AND (nombre ILIKE CONCAT('%', :term, '%') OR codigo ILIKE CONCAT('%', :term, '%'))
        ORDER BY nombre
        LIMIT 20
        """, nativeQuery = true)
    List<Producto> buscar(@Param("term") String term);

    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.cantidadStock <= p.stockMinimo ORDER BY p.nombre")
    List<Producto> findStockBajo();
}
