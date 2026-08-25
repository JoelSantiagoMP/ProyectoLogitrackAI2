package com.example.logitrack.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class OrdenCompraRequest {

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @NotNull(message = "El proveedor es obligatorio")
    private Long proveedorId;

    @NotNull(message = "La bodega destino es obligatoria")
    private Long bodegaDestinoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor que 0")
    private Integer cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    private Double precioUnitario;

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Long proveedorId) {
        this.proveedorId = proveedorId;
    }

    public Long getBodegaDestinoId() {
        return bodegaDestinoId;
    }

    public void setBodegaDestinoId(Long bodegaDestinoId) {
        this.bodegaDestinoId = bodegaDestinoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
}
