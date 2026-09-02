package com.panaderia.erp.reportes.dto;

import java.util.List;

public record StockCriticoResponse(
        List<StockCriticoItemResponse> productos,
        List<StockCriticoItemResponse> insumos
) {
}
