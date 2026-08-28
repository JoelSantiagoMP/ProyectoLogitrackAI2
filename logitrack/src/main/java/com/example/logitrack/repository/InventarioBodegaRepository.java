package com.example.logitrack.repository;

import com.example.logitrack.model.InventarioBodega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioBodegaRepository extends JpaRepository<InventarioBodega, Long> {

    /**
     * Busca la relación de stock de un producto específico dentro de una bodega.
     */
    Optional<InventarioBodega> findByBodegaIdAndProductoId(Long bodegaId, Long productoId);

    List<InventarioBodega> findByProductoId(Long productoId);

    @Query("SELECT i FROM InventarioBodega i JOIN FETCH i.bodega WHERE i.producto.id IN :productoIds")
    List<InventarioBodega> findByProductoIdIn(@Param("productoIds") List<Long> productoIds);

    /**
     * Obtiene todos los registros de inventario de una bodega específica.
     */
    List<InventarioBodega> findByBodegaId(Long bodegaId);

    /**
     * Obtiene el listado de inventarios cuyo stock sea inferior a un límite dado.
     * Cumple con el requerimiento de consulta de productos con stock bajo (< 10).
     */
    @Query("SELECT i FROM InventarioBodega i WHERE i.cantidad < :limiteStock")
    List<InventarioBodega> findStockBajo(@Param("limiteStock") Integer limiteStock);

    /**
     * Calcula la suma total de existencias de un producto distribuidas en todas las
     * bodegas.
     */
    @Query("SELECT COALESCE(SUM(i.cantidad), 0) FROM InventarioBodega i WHERE i.producto.id = :productoId")
    Integer obtenerStockTotalPorProducto(@Param("productoId") Long productoId);

    @Query("SELECT COALESCE(SUM(i.cantidad), 0) FROM InventarioBodega i WHERE i.bodega.id = :bodegaId")
    Integer obtenerStockTotalPorBodega(@Param("bodegaId") Long bodegaId);

    @Query("""
            SELECT i FROM InventarioBodega i
            JOIN FETCH i.bodega
            JOIN FETCH i.producto
            WHERE (:bodegaId IS NULL OR i.bodega.id = :bodegaId)
              AND (:productoId IS NULL OR i.producto.id = :productoId)
              AND (:filtrarCategoria = false OR LOWER(i.producto.categoria) = :categoria)
            ORDER BY i.bodega.nombre, i.producto.nombre
            """)
    List<InventarioBodega> findInventarioParaReporte(
            @Param("bodegaId") Long bodegaId,
            @Param("productoId") Long productoId,
            @Param("filtrarCategoria") boolean filtrarCategoria,
            @Param("categoria") String categoria);
}