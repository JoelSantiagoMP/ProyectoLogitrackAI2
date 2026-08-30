package com.example.logitrack.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Stock total de un producto y desglose por bodega (calculado desde movimientos)")
public class StockProductoResponse {

    @Schema(example = "12")
    private Long productoId;

    @Schema(example = "85")
    private int stockTotal;

    @Schema(description = "Existencias del producto en cada bodega")
    private List<StockBodegaItemResponse> porBodega = new ArrayList<>();

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public int getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(int stockTotal) {
        this.stockTotal = stockTotal;
    }

    public List<StockBodegaItemResponse> getPorBodega() {
        return porBodega;
    }

    public void setPorBodega(List<StockBodegaItemResponse> porBodega) {
        this.porBodega = porBodega;
    }
}
