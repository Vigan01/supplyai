package com.supplyai.purchase;

import static com.supplyai.purchase.PurchaseOrderDtos.PurchaseOrderRequest;
import static com.supplyai.purchase.PurchaseOrderDtos.PurchaseOrderResponse;

import java.util.List;

import com.supplyai.activity.ActivityLogService;
import com.supplyai.inventory.Product;
import com.supplyai.inventory.ProductService;
import com.supplyai.supplier.Supplier;
import com.supplyai.supplier.SupplierService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository orders;
    private final ProductService products;
    private final SupplierService suppliers;
    private final ActivityLogService activities;

    PurchaseOrderService(PurchaseOrderRepository orders, ProductService products, SupplierService suppliers,
            ActivityLogService activities) {
        this.orders = orders;
        this.products = products;
        this.suppliers = suppliers;
        this.activities = activities;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> listOrders() {
        return orders.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PurchaseOrderResponse createOrder(PurchaseOrderRequest request) {
        Product product = products.findProduct(request.productId());
        Supplier supplier = request.supplierId() == null ? product.getSupplier() : suppliers.findSupplier(request.supplierId());
        PurchaseOrder order = orders.save(new PurchaseOrder(
                product,
                supplier,
                request.quantity(),
                PurchaseOrderStatus.PLANNED,
                request.expectedDeliveryDate(),
                cleanNote(request.note())
        ));
        activities.record("CREATE", "PURCHASE_ORDER", order.getId(), "Nachbestellung erstellt",
                product.getSku() + ": " + order.getQuantity() + " Einheiten vorgemerkt.");
        return toResponse(order);
    }

    @Transactional
    public PurchaseOrderResponse updateStatus(Long id, PurchaseOrderStatus status) {
        PurchaseOrder order = orders.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nachbestellung wurde nicht gefunden."));
        order.updateStatus(status);
        activities.record("UPDATE", "PURCHASE_ORDER", order.getId(), "Nachbestellung aktualisiert",
                order.getProduct().getSku() + " steht jetzt auf " + status + ".");
        return toResponse(order);
    }

    public PurchaseOrderResponse toResponse(PurchaseOrder order) {
        Product product = order.getProduct();
        Supplier supplier = order.getSupplier();
        return new PurchaseOrderResponse(
                order.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                supplier == null ? null : supplier.getId(),
                supplier == null ? "Kein Lieferant" : supplier.getName(),
                order.getQuantity(),
                order.getStatus(),
                order.getExpectedDeliveryDate(),
                order.getNote(),
                order.getCreatedAt().toString(),
                order.getUpdatedAt().toString()
        );
    }

    private String cleanNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }
}
