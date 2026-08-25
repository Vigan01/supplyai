package com.supplyai.purchase;

import static com.supplyai.purchase.PurchaseOrderDtos.PurchaseOrderRequest;
import static com.supplyai.purchase.PurchaseOrderDtos.PurchaseOrderResponse;
import static com.supplyai.purchase.PurchaseOrderDtos.PurchaseOrderStatusRequest;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-orders")
class PurchaseOrderController {

    private final PurchaseOrderService orders;

    PurchaseOrderController(PurchaseOrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    List<PurchaseOrderResponse> listOrders() {
        return orders.listOrders();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PurchaseOrderResponse createOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        return orders.createOrder(request);
    }

    @PatchMapping("/{id}/status")
    PurchaseOrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody PurchaseOrderStatusRequest request) {
        return orders.updateStatus(id, request.status());
    }
}
