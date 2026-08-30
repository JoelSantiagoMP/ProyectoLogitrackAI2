package com.example.logitrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Producto en riesgo: stock por debajo del punto de reorden (con proveedor principal)")
public class ProductoRiesgoResponse {

    @Schema(example = "12")
    private Long productoId;

    @Schema(example = "Tornillo M8")
    private String nombreProducto;

    @Schema(example = "3")
    private Long proveedorId;

    @Schema(example = "8")
    private int stockTotal;

    @Schema(description = "Promedio de unidades en SALIDAS de los últimos 30 días / 30", example = "2.5")
    private double consumoDiarioPromedio;

    @Schema(description = "consumoDiarioPromedio × diasEntrega × 1.5", example = "45.0")
    private double puntoReorden;

    @Schema(description = "stockTotal / consumoDiarioPromedio; null si consumo es 0", example = "3.2", nullable = true)
    private Double diasCobertura;

    @Schema(description = "CON_CONSUMO o SIN_CONSUMO", example = "CON_CONSUMO")
    private String estadoCobertura;

    @Schema(description = "Bodega con menor stock del producto; empate → menor id", example = "2")
    private Long bodegaDestinoId;

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Long proveedorId) {
        this.proveedorId = proveedorId;
    }

    public int getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(int stockTotal) {
        this.stockTotal = stockTotal;
    }

    public double getConsumoDiarioPromedio() {
        return consumoDiarioPromedio;
    }

    public void setConsumoDiarioPromedio(double consumoDiarioPromedio) {
        this.consumoDiarioPromedio = consumoDiarioPromedio;
    }

    public double getPuntoReorden() {
        return puntoReorden;
    }

    public void setPuntoReorden(double puntoReorden) {
        this.puntoReorden = puntoReorden;
    }

    public Double getDiasCobertura() {
        return diasCobertura;
    }

    public void setDiasCobertura(Double diasCobertura) {
        this.diasCobertura = diasCobertura;
    }

    public String getEstadoCobertura() {
        return estadoCobertura;
    }

    public void setEstadoCobertura(String estadoCobertura) {
        this.estadoCobertura = estadoCobertura;
    }

    public Long getBodegaDestinoId() {
        return bodegaDestinoId;
    }

    public void setBodegaDestinoId(Long bodegaDestinoId) {
        this.bodegaDestinoId = bodegaDestinoId;
    }
}
