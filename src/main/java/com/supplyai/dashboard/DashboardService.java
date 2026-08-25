package com.supplyai.dashboard;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import com.supplyai.inventory.Product;
import com.supplyai.inventory.ProductRepository;
import com.supplyai.inventory.ProductService;
import com.supplyai.purchase.PurchaseOrderRepository;
import com.supplyai.purchase.PurchaseOrderStatus;
import com.supplyai.supplier.SupplierRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final ProductService productService;
    private final PurchaseOrderRepository purchaseOrders;

    DashboardService(ProductRepository products, SupplierRepository suppliers, ProductService productService,
            PurchaseOrderRepository purchaseOrders) {
        this.products = products;
        this.suppliers = suppliers;
        this.productService = productService;
        this.purchaseOrders = purchaseOrders;
    }

    @Transactional(readOnly = true)
    public DashboardOverview overview() {
        List<Product> allProducts = products.findAll();
        long critical = allProducts.stream()
                .filter(product -> product.getStock() < product.getReorderPoint())
                .count();
        BigDecimal inventoryValue = allProducts.stream()
                .map(product -> product.getUnitCost().multiply(BigDecimal.valueOf(product.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long delayedSuppliers = suppliers.findAll().stream()
                .filter(supplier -> supplier.isDelayed())
                .count();
        long openOrders = purchaseOrders.findAll().stream()
                .filter(order -> order.getStatus() == PurchaseOrderStatus.PLANNED
                        || order.getStatus() == PurchaseOrderStatus.ORDERED)
                .count();

        return new DashboardOverview(
                List.of(
                        new Metric("Aktive Produkte", String.valueOf(allProducts.size()), "SKU-Katalog gepflegt"),
                        new Metric("Kritische Bestände", String.valueOf(critical), critical == 0 ? "Keine Sofortmaßnahmen" : "Nachbestellung prüfen"),
                        new Metric("Lieferanten", String.valueOf(suppliers.count()), delayedSuppliers + " mit Verzögerung"),
                        new Metric("Offene Bestellungen", String.valueOf(openOrders), "geplant oder bestellt")
                ),
                allProducts.stream()
                        .sorted(Comparator.comparing(Product::getSku))
                        .map(productService::toResponse)
                        .toList(),
                buildAlerts(allProducts),
                productService.recentMovements()
        );
    }

    private List<Alert> buildAlerts(List<Product> allProducts) {
        return allProducts.stream()
                .filter(product -> product.getStock() < product.getReorderPoint() || productService.daysOfCover(product) <= 14)
                .sorted(Comparator.comparingInt(product -> product.getStock() - product.getReorderPoint()))
                .limit(8)
                .map(product -> {
                    int days = productService.daysOfCover(product);
                    if (product.getStock() < product.getReorderPoint()) {
                        return new Alert("high", product.getName() + " liegt unter dem Meldebestand. Empfehlung: "
                                + productService.reorderQuantity(product) + " Einheiten bestellen.");
                    }
                    return new Alert("medium", product.getName() + " reicht bei aktueller Nachfrage noch ca. "
                            + days + " Tage.");
                })
                .toList();
    }

    private String formatCurrency(BigDecimal value) {
        return "EUR " + value.setScale(2).toPlainString();
    }

    public record DashboardOverview(
            List<Metric> metrics,
            List<com.supplyai.inventory.ProductDtos.ProductResponse> inventory,
            List<Alert> alerts,
            List<com.supplyai.inventory.ProductDtos.MovementResponse> movements) {
    }

    public record Metric(String label, String value, String trend) {
    }

    public record Alert(String severity, String message) {
    }
}
