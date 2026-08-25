package com.supplyai.supplier;

import static com.supplyai.supplier.SupplierDtos.SupplierRequest;
import static com.supplyai.supplier.SupplierDtos.SupplierResponse;

import java.util.Comparator;
import java.util.List;

import com.supplyai.activity.ActivityLogService;
import com.supplyai.inventory.ProductRepository;
import com.supplyai.purchase.PurchaseOrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

    private final SupplierRepository suppliers;
    private final ActivityLogService activities;
    private final ProductRepository products;
    private final PurchaseOrderRepository purchaseOrders;

    SupplierService(SupplierRepository suppliers, ActivityLogService activities, ProductRepository products,
            PurchaseOrderRepository purchaseOrders) {
        this.suppliers = suppliers;
        this.activities = activities;
        this.products = products;
        this.purchaseOrders = purchaseOrders;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> listSuppliers() {
        return suppliers.findAll().stream()
                .sorted(Comparator.comparing(Supplier::getName))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        suppliers.findByName(request.name().trim()).ifPresent(existing -> {
            throw new IllegalArgumentException("Dieser Lieferant existiert bereits.");
        });
        Supplier supplier = suppliers.save(new Supplier(
                request.name().trim(),
                blankToNull(request.email()),
                request.leadTimeDays(),
                request.delayed()
        ));
        activities.record("CREATE", "SUPPLIER", supplier.getId(), "Lieferant erstellt",
                supplier.getName() + " wurde angelegt.");
        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = findSupplier(id);
        suppliers.findByName(request.name().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Dieser Lieferant existiert bereits.");
                });
        supplier.update(request.name().trim(), blankToNull(request.email()), request.leadTimeDays(), request.delayed());
        activities.record("UPDATE", "SUPPLIER", supplier.getId(), "Lieferant aktualisiert",
                supplier.getName() + " wurde aktualisiert.");
        return toResponse(supplier);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = findSupplier(id);
        products.findBySupplierId(supplier.getId()).forEach(product -> product.clearSupplier());
        purchaseOrders.findBySupplierId(supplier.getId()).forEach(order -> order.clearSupplier());
        activities.record("DELETE", "SUPPLIER", supplier.getId(), "Lieferant gelöscht",
                supplier.getName() + " wurde gelöscht.");
        suppliers.delete(supplier);
    }

    public Supplier findSupplier(Long id) {
        return suppliers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lieferant wurde nicht gefunden."));
    }

    public SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getEmail(),
                supplier.getLeadTimeDays(),
                supplier.isDelayed()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
