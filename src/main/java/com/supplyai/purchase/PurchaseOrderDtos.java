package com.supplyai.purchase;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class PurchaseOrderDtos {

    private PurchaseOrderDtos() {
    }

    public record PurchaseOrderRequest(
            @NotNull Long productId,
            Long supplierId,
            @Min(1) int quantity,
            LocalDate expectedDeliveryDate,
            String note) {
    }

    public record PurchaseOrderStatusRequest(@NotNull PurchaseOrderStatus status) {
    }

    public record PurchaseOrderResponse(
            Long id,
            Long productId,
            String sku,
            String productName,
            Long supplierId,
            String supplierName,
            int quantity,
            PurchaseOrderStatus status,
            LocalDate expectedDeliveryDate,
            String note,
            String createdAt,
            String updatedAt) {
    }
}
