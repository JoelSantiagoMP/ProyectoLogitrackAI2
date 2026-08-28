package com.example.logitrack.service;

import com.example.logitrack.dto.ResultadoCobertura;
import com.example.logitrack.model.Bodega;
import com.example.logitrack.model.EstadoOrdenCompra;
import com.example.logitrack.model.InventarioBodega;
import com.example.logitrack.model.Movimiento;
import com.example.logitrack.model.OrdenCompra;
import com.example.logitrack.model.Producto;
import com.example.logitrack.model.Proveedor;
import com.example.logitrack.model.TipoMovimiento;
import com.example.logitrack.repository.BodegaRepository;
import com.example.logitrack.repository.InventarioBodegaRepository;
import com.example.logitrack.repository.MovimientoRepository;
import com.example.logitrack.repository.OrdenCompraRepository;
import com.example.logitrack.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IndicadoresInventarioService {

    public static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private final ProductoRepository productoRepository;
    private final BodegaRepository bodegaRepository;
    private final InventarioBodegaRepository inventarioBodegaRepository;
    private final MovimientoRepository movimientoRepository;
    private final OrdenCompraRepository ordenCompraRepository;

    public IndicadoresInventarioService() {
        this(null, null, null, null, null);
    }

    @Autowired
    public IndicadoresInventarioService(ProductoRepository productoRepository,
            BodegaRepository bodegaRepository,
            InventarioBodegaRepository inventarioBodegaRepository,
            MovimientoRepository movimientoRepository,
            OrdenCompraRepository ordenCompraRepository) {
        this.productoRepository = productoRepository;
        this.bodegaRepository = bodegaRepository;
        this.inventarioBodegaRepository = inventarioBodegaRepository;
        this.movimientoRepository = movimientoRepository;
        this.ordenCompraRepository = ordenCompraRepository;
    }

    public ResultadoCobertura calcularCobertura(int stockTotal, double consumoDiarioPromedio) {
        if (consumoDiarioPromedio <= 0) {
            return new ResultadoCobertura(null, "SIN_CONSUMO");
        }
        return new ResultadoCobertura(stockTotal / consumoDiarioPromedio, "CON_CONSUMO");
    }

    public boolean estaEnRiesgo(int stockTotal, double puntoReorden, boolean tieneProveedorPrincipal) {
        if (!tieneProveedorPrincipal) {
            return false;
        }
        if (stockTotal <= 0) {
            return true;
        }
        return stockTotal < puntoReorden;
    }

    public double puntoReorden(double consumoDiarioPromedio, int diasEntrega) {
        return consumoDiarioPromedio * diasEntrega * 1.5;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerKpis() {
        OffsetDateTime calculadoEn = OffsetDateTime.now(ZONA_BOGOTA);
        List<Map<String, Object>> ocupacion = new ArrayList<>();
        for (Bodega bodega : bodegaRepository.findAllByOrderByIdAsc()) {
            int unidades = stockAlmacenadoEnBodega(bodega.getId());
            double porcentaje = calcularPorcentajeOcupacion(unidades, bodega.getCapacidad());
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("bodegaId", bodega.getId());
            fila.put("nombre", bodega.getNombre());
            fila.put("porcentaje", porcentaje);
            fila.put("unidades", unidades);
            fila.put("capacidad", bodega.getCapacidad());
            ocupacion.add(fila);
        }

        int quiebre = 0;
        int riesgo = 0;
        for (Producto producto : productoRepository.findAll()) {
            int stockTotal = stockTotalProducto(producto.getId());
            if (stockTotal == 0) {
                quiebre++;
            }
            if (evaluarRiesgo(producto, stockTotal)) {
                riesgo++;
            }
        }

        List<OrdenCompra> borradores = ordenCompraRepository.findByEstado(EstadoOrdenCompra.BORRADOR);
        double monto = borradores.stream().mapToDouble(o -> o.getTotal() != null ? o.getTotal() : 0.0).sum();

        Map<String, Object> ordenesPorAprobar = new LinkedHashMap<>();
        ordenesPorAprobar.put("cantidad", borradores.size());
        ordenesPorAprobar.put("montoTotal", monto);

        LocalDate ayer = LocalDate.now(ZONA_BOGOTA).minusDays(1);
        List<Movimiento> deAyer = movimientosEnRangoCalendarioBogota(ayer, ayer.plusDays(1));
        long entrada = deAyer.stream().filter(m -> m.getTipoMovimiento() == TipoMovimiento.ENTRADA).count();
        long salida = deAyer.stream().filter(m -> m.getTipoMovimiento() == TipoMovimiento.SALIDA).count();
        long transferencia = deAyer.stream().filter(m -> m.getTipoMovimiento() == TipoMovimiento.TRANSFERENCIA).count();

        Map<String, Object> movimientosAyer = new LinkedHashMap<>();
        movimientosAyer.put("entrada", entrada);
        movimientosAyer.put("salida", salida);
        movimientosAyer.put("transferencia", transferencia);
        movimientosAyer.put("fechaReferencia", ayer.toString());

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("calculadoEn", calculadoEn);
        kpis.put("ocupacionPorBodega", ocupacion);
        kpis.put("productosEnQuiebre", quiebre);
        kpis.put("productosEnRiesgo", riesgo);
        kpis.put("ordenesPorAprobar", ordenesPorAprobar);
        kpis.put("movimientosAyer", movimientosAyer);
        return kpis;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerStockProducto(Long productoId) {
        productoRepository.findById(productoId)
                .orElseThrow(() -> new com.example.logitrack.exception.ResourceNotFoundException(
                        "Producto no encontrado con el id: " + productoId));
        int total = stockTotalProducto(productoId);
        List<Map<String, Object>> porBodega = new ArrayList<>();
        for (Bodega bodega : bodegaRepository.findAllByOrderByIdAsc()) {
            porBodega.add(Map.of(
                    "bodegaId", bodega.getId(),
                    "nombre", bodega.getNombre(),
                    "cantidad", stockEnBodega(productoId, bodega.getId())));
        }
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("productoId", productoId);
        respuesta.put("stockTotal", total);
        respuesta.put("porBodega", porBodega);
        return respuesta;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarProductosEnRiesgo() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Producto producto : productoRepository.findAll()) {
            int stockTotal = stockTotalProducto(producto.getId());
            if (!evaluarRiesgo(producto, stockTotal)) {
                continue;
            }
            Proveedor proveedor = producto.getProveedorPrincipal();
            if (proveedor == null || proveedor.getId() == null) {
                continue;
            }
            double consumo = consumoDiarioPromedio(producto.getId());
            int diasEntrega = proveedor.getDiasEntrega() != null ? proveedor.getDiasEntrega() : 0;
            double punto = puntoReorden(consumo, diasEntrega);
            ResultadoCobertura cobertura = calcularCobertura(stockTotal, consumo);
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("productoId", producto.getId());
            fila.put("nombreProducto", producto.getNombre());
            fila.put("proveedorId", proveedor.getId());
            fila.put("stockTotal", stockTotal);
            fila.put("consumoDiarioPromedio", consumo);
            fila.put("puntoReorden", punto);
            fila.put("diasCobertura", cobertura.getDiasCobertura());
            fila.put("estadoCobertura", cobertura.getEstadoCobertura());
            fila.put("bodegaDestinoId", sugerirBodegaDestinoId(producto.getId()));
            resultado.add(fila);
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarBodegasCriticas() {
        List<Map<String, Object>> criticas = new ArrayList<>();
        for (Bodega bodega : bodegaRepository.findAllByOrderByIdAsc()) {
            int unidades = stockAlmacenadoEnBodega(bodega.getId());
            if (bodega.getCapacidad() == null || bodega.getCapacidad() == 0) {
                continue;
            }
            double porcentaje = calcularPorcentajeOcupacion(unidades, bodega.getCapacidad());
            if (porcentaje >= 90.0) {
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("bodegaId", bodega.getId());
                fila.put("nombre", bodega.getNombre());
                fila.put("porcentaje", porcentaje);
                fila.put("unidades", unidades);
                fila.put("capacidad", bodega.getCapacidad());
                criticas.add(fila);
            }
        }
        return criticas;
    }

    public Long sugerirBodegaDestinoId(Long productoId) {
        List<Bodega> bodegas = bodegaRepository.findAllByOrderByIdAsc();
        if (bodegas.isEmpty()) {
            return null;
        }
        return bodegas.stream()
                .sorted(Comparator
                        .comparingInt((Bodega b) -> stockEnBodega(productoId, b.getId()))
                        .thenComparing(Bodega::getId))
                .map(Bodega::getId)
                .findFirst()
                .orElse(null);
    }

    public double consumoDiarioPromedio(Long productoId) {
        LocalDate hoy = LocalDate.now(ZONA_BOGOTA);
        LocalDate desde = hoy.minusDays(29);
        LocalDate hastaExclusivo = hoy.plusDays(1);
        long unidades = 0;
        for (Movimiento movimiento : movimientosEnRangoCalendarioBogota(desde, hastaExclusivo)) {
            if (movimiento.getTipoMovimiento() != TipoMovimiento.SALIDA) {
                continue;
            }
            if (movimiento.getDetalles() == null) {
                continue;
            }
            unidades += movimiento.getDetalles().stream()
                    .filter(d -> d.getProducto() != null && productoId.equals(d.getProducto().getId()))
                    .mapToLong(d -> d.getCantidad() != null ? d.getCantidad() : 0)
                    .sum();
        }
        return unidades / 30.0;
    }

    public int stockTotalProducto(Long productoId) {
        Integer total = inventarioBodegaRepository.obtenerStockTotalPorProducto(productoId);
        if (total != null) {
            return total;
        }
        return stockTotalDesdeMovimientos(productoId);
    }

    public int stockEnBodega(Long productoId, Long bodegaId) {
        return inventarioBodegaRepository.findByBodegaIdAndProductoId(bodegaId, productoId)
                .map(InventarioBodega::getCantidad)
                .orElseGet(() -> stockEnBodegaDesdeMovimientos(productoId, bodegaId));
    }

    private boolean evaluarRiesgo(Producto producto, int stockTotal) {
        Proveedor proveedor = producto.getProveedorPrincipal();
        if (proveedor == null) {
            return false;
        }
        if (stockTotal <= 0) {
            return true;
        }
        if (proveedor.getDiasEntrega() == null) {
            return false;
        }
        double consumo = consumoDiarioPromedio(producto.getId());
        double punto = puntoReorden(consumo, proveedor.getDiasEntrega());
        return estaEnRiesgo(stockTotal, punto, true);
    }

    private int stockAlmacenadoEnBodega(Long bodegaId) {
        Integer total = inventarioBodegaRepository.obtenerStockTotalPorBodega(bodegaId);
        return total != null ? total : 0;
    }

    double calcularPorcentajeOcupacion(int unidades, Integer capacidad) {
        if (capacidad == null || capacidad <= 0) {
            return 0.0;
        }
        return (unidades * 100.0) / capacidad;
    }

    private List<Movimiento> movimientosEnRangoCalendarioBogota(LocalDate desdeInclusive, LocalDate hastaExclusive) {
        return movimientoRepository.findByFechaCalendarioBogota(desdeInclusive, hastaExclusive);
    }

    private int stockTotalDesdeMovimientos(Long productoId) {
        Map<Long, Integer> porBodega = new HashMap<>();
        for (Bodega bodega : bodegaRepository.findAllByOrderByIdAsc()) {
            porBodega.put(bodega.getId(), stockEnBodegaDesdeMovimientos(productoId, bodega.getId()));
        }
        return porBodega.values().stream().mapToInt(Integer::intValue).sum();
    }

    private int stockEnBodegaDesdeMovimientos(Long productoId, Long bodegaId) {
        int stock = 0;
        for (Movimiento movimiento : movimientoRepository.findAll()) {
            if (movimiento.getDetalles() == null) {
                continue;
            }
            for (var detalle : movimiento.getDetalles()) {
                if (detalle.getProducto() == null || !productoId.equals(detalle.getProducto().getId())) {
                    continue;
                }
                int cantidad = detalle.getCantidad() != null ? detalle.getCantidad() : 0;
                TipoMovimiento tipo = movimiento.getTipoMovimiento();
                if (tipo == TipoMovimiento.ENTRADA && movimiento.getBodegaDestino() != null
                        && bodegaId.equals(movimiento.getBodegaDestino().getId())) {
                    stock += cantidad;
                } else if (tipo == TipoMovimiento.SALIDA && movimiento.getBodegaOrigen() != null
                        && bodegaId.equals(movimiento.getBodegaOrigen().getId())) {
                    stock -= cantidad;
                } else if (tipo == TipoMovimiento.TRANSFERENCIA) {
                    if (movimiento.getBodegaOrigen() != null && bodegaId.equals(movimiento.getBodegaOrigen().getId())) {
                        stock -= cantidad;
                    }
                    if (movimiento.getBodegaDestino() != null
                            && bodegaId.equals(movimiento.getBodegaDestino().getId())) {
                        stock += cantidad;
                    }
                }
            }
        }
        return stock;
    }
}
