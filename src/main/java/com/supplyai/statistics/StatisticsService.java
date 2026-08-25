package com.supplyai.statistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.supplyai.inventory.InventoryMovement;
import com.supplyai.inventory.InventoryMovementRepository;
import com.supplyai.inventory.Product;
import com.supplyai.inventory.ProductRepository;
import com.supplyai.inventory.ProductService;
import com.supplyai.purchase.PurchaseOrderRepository;
import com.supplyai.purchase.PurchaseOrderStatus;
import com.supplyai.supplier.SupplierRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {

    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final InventoryMovementRepository movements;
    private final ProductService productService;
    private final PurchaseOrderRepository purchaseOrders;

    StatisticsService(ProductRepository products, SupplierRepository suppliers,
            InventoryMovementRepository movements, ProductService productService, PurchaseOrderRepository purchaseOrders) {
        this.products = products;
        this.suppliers = suppliers;
        this.movements = movements;
        this.productService = productService;
        this.purchaseOrders = purchaseOrders;
    }

    @Transactional(readOnly = true)
    public StatisticsOverview overview() {
        List<Product> allProducts = products.findAll();
        List<InventoryMovement> recentMovements = movements.findTop20ByOrderByCreatedAtDesc();

        BigDecimal inventoryValue = allProducts.stream()
                .map(product -> product.getUnitCost().multiply(BigDecimal.valueOf(product.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        int inbound = recentMovements.stream()
                .filter(movement -> movement.getQuantityChange() > 0)
                .mapToInt(InventoryMovement::getQuantityChange)
                .sum();
        int outbound = recentMovements.stream()
                .filter(movement -> movement.getQuantityChange() < 0)
                .mapToInt(movement -> Math.abs(movement.getQuantityChange()))
                .sum();
        double averageCover = allProducts.stream()
                .mapToInt(productService::daysOfCover)
                .filter(days -> days < 999)
                .average()
                .orElse(0);
        long openOrders = purchaseOrders.findAll().stream()
                .filter(order -> order.getStatus() == PurchaseOrderStatus.PLANNED
                        || order.getStatus() == PurchaseOrderStatus.ORDERED)
                .count();

        return new StatisticsOverview(
                List.of(
                        new StatCard("Lagerwert", "EUR " + inventoryValue.toPlainString(), "Summe Bestand x Stückkosten"),
                        new StatCard("Durchschnittliche Reichweite", Math.round(averageCover) + " Tage", "ohne Produkte ohne Bedarf"),
                        new StatCard("Wareneingang", String.valueOf(inbound), "letzte 20 Bewegungen"),
                        new StatCard("Offene Bestellungen", String.valueOf(openOrders), "geplant oder bestellt")
                ),
                distributionByStatus(allProducts),
                distributionByCategory(allProducts),
                allProducts.stream()
                        .sorted(Comparator.comparingInt(productService::daysOfCover))
                        .limit(5)
                        .map(product -> new RiskItem(
                                product.getSku(),
                                product.getName(),
                                productService.status(product, productService.daysOfCover(product)),
                                productService.daysOfCover(product),
                                productService.reorderQuantity(product)
                        ))
                        .toList(),
                new SupplierRisk(suppliers.count(), suppliers.findAll().stream().filter(supplier -> supplier.isDelayed()).count())
        );
    }

    private List<DistributionItem> distributionByStatus(List<Product> allProducts) {
        return allProducts.stream()
                .collect(Collectors.groupingBy(
                        product -> productService.status(product, productService.daysOfCover(product)),
                        LinkedHashMap::new,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> new DistributionItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<DistributionItem> distributionByCategory(List<Product> allProducts) {
        Map<String, Long> grouped = allProducts.stream()
                .collect(Collectors.groupingBy(Product::getCategory, LinkedHashMap::new, Collectors.counting()));
        return grouped.entrySet().stream()
                .map(entry -> new DistributionItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    public record StatisticsOverview(
            List<StatCard> cards,
            List<DistributionItem> statusDistribution,
            List<DistributionItem> categoryDistribution,
            List<RiskItem> topRisks,
            SupplierRisk supplierRisk) {
    }

    public record StatCard(String label, String value, String detail) {
    }

    public record DistributionItem(String label, long value) {
    }

    public record RiskItem(String sku, String name, String status, int daysOfCover, int reorderQuantity) {
    }

    public record SupplierRisk(long totalSuppliers, long delayedSuppliers) {
    }
}
