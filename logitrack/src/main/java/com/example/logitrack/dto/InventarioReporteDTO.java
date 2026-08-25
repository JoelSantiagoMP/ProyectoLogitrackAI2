package com.example.logitrack.dto;

public class InventarioReporteDTO {

    private Long bodegaId;
    private String bodegaNombre;
    private Long productoId;
    private String productoNombre;
    private String categoria;
    private Integer cantidad;

    public InventarioReporteDTO() {
    }

    public InventarioReporteDTO(Long bodegaId, String bodegaNombre, Long productoId,
            String productoNombre, String categoria, Integer cantidad) {
        this.bodegaId = bodegaId;
        this.bodegaNombre = bodegaNombre;
        this.productoId = productoId;
        this.productoNombre = productoNombre;
        this.categoria = categoria;
        this.cantidad = cantidad;
    }

    public Long getBodegaId() {
        return bodegaId;
    }

    public void setBodegaId(Long bodegaId) {
        this.bodegaId = bodegaId;
    }

    public String getBodegaNombre() {
        return bodegaNombre;
    }

    public void setBodegaNombre(String bodegaNombre) {
        this.bodegaNombre = bodegaNombre;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public void setProductoNombre(String productoNombre) {
        this.productoNombre = productoNombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}
