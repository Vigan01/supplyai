package com.supplyai.inventory;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record ProductRequest(
            @NotBlank String sku,
            @NotBlank String name,
            @NotBlank String category,
            @Min(0) int stock,
            @Min(0) int reorderPoint,
            @Min(0) int averageDailyDemand,
            @NotNull @DecimalMin("0.00") BigDecimal unitCost,
            Long supplierId) {
    }

    public record ProductResponse(
            Long id,
            String sku,
            String name,
            String category,
            int stock,
            int reorderPoint,
            int averageDailyDemand,
            BigDecimal unitCost,
            Long supplierId,
            String supplierName,
            String status,
            int daysOfCover,
            int reorderQuantity,
            BigDecimal inventoryValue) {
    }

    public record MovementRequest(
            @Min(1) int quantity,
            @NotBlank String type,
            @NotBlank String reason) {
    }

    public record MovementResponse(
            Long id,
            Long productId,
            String sku,
            String productName,
            int quantityChange,
            String reason,
            String createdAt) {
    }
}
