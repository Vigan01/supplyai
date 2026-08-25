package com.supplyai.inventory;

import static com.supplyai.inventory.ProductDtos.MovementRequest;
import static com.supplyai.inventory.ProductDtos.MovementResponse;
import static com.supplyai.inventory.ProductDtos.ProductRequest;
import static com.supplyai.inventory.ProductDtos.ProductResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import com.supplyai.activity.ActivityLogService;
import com.supplyai.supplier.Supplier;
import com.supplyai.supplier.SupplierRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final InventoryMovementRepository movements;
    private final ActivityLogService activities;

    ProductService(ProductRepository products, SupplierRepository suppliers, InventoryMovementRepository movements,
            ActivityLogService activities) {
        this.products = products;
        this.suppliers = suppliers;
        this.movements = movements;
        this.activities = activities;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts() {
        return products.findAll().stream()
                .sorted(Comparator.comparing(Product::getSku))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (products.existsBySku(request.sku())) {
            throw new IllegalArgumentException("Diese SKU existiert bereits.");
        }
        Supplier supplier = findSupplier(request.supplierId());
        Product product = new Product(
                request.sku().trim(),
                request.name().trim(),
                request.category().trim(),
                request.stock(),
                request.reorderPoint(),
                request.averageDailyDemand(),
                request.unitCost(),
                supplier
        );
        Product saved = products.save(product);
        activities.record("CREATE", "PRODUCT", saved.getId(), "Produkt erstellt",
                saved.getSku() + " - " + saved.getName() + " wurde angelegt.");
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProduct(id);
        products.findBySku(request.sku().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Diese SKU existiert bereits.");
                });
        product.update(
                request.sku().trim(),
                request.name().trim(),
                request.category().trim(),
                request.stock(),
                request.reorderPoint(),
                request.averageDailyDemand(),
                request.unitCost(),
                findSupplier(request.supplierId())
        );
        activities.record("UPDATE", "PRODUCT", product.getId(), "Produkt aktualisiert",
                product.getSku() + " - " + product.getName() + " wurde aktualisiert.");
        return toResponse(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProduct(id);
        activities.record("DELETE", "PRODUCT", product.getId(), "Produkt gelöscht",
                product.getSku() + " - " + product.getName() + " wurde gelöscht.");
        movements.deleteByProductId(product.getId());
        products.delete(product);
    }

    @Transactional
    public MovementResponse applyMovement(Long productId, MovementRequest request) {
        Product product = findProduct(productId);
        int quantityChange = switch (request.type().trim().toUpperCase()) {
            case "IN" -> request.quantity();
            case "OUT" -> -request.quantity();
            case "ADJUSTMENT" -> request.quantity() - product.getStock();
            default -> throw new IllegalArgumentException("Bewegungstyp muss IN, OUT oder ADJUSTMENT sein.");
        };
        product.applyMovement(quantityChange);
        InventoryMovement movement = movements.save(new InventoryMovement(product, quantityChange, request.reason().trim()));
        activities.record("MOVEMENT", "PRODUCT", product.getId(), "Bestandsbewegung gebucht",
                product.getSku() + ": " + signed(quantityChange) + " Einheiten. Grund: " + request.reason().trim());
        return toMovementResponse(movement);
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> recentMovements() {
        return movements.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::toMovementResponse)
                .toList();
    }

    public Product findProduct(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produkt wurde nicht gefunden."));
    }

    public ProductResponse toResponse(Product product) {
        Supplier supplier = product.getSupplier();
        int daysOfCover = daysOfCover(product);
        int reorderQuantity = reorderQuantity(product);
        BigDecimal inventoryValue = product.getUnitCost()
                .multiply(BigDecimal.valueOf(product.getStock()))
                .setScale(2, RoundingMode.HALF_UP);
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getStock(),
                product.getReorderPoint(),
                product.getAverageDailyDemand(),
                product.getUnitCost(),
                supplier == null ? null : supplier.getId(),
                supplier == null ? "Kein Lieferant" : supplier.getName(),
                status(product, daysOfCover),
                daysOfCover,
                reorderQuantity,
                inventoryValue
        );
    }

    public int daysOfCover(Product product) {
        if (product.getAverageDailyDemand() == 0) {
            return 999;
        }
        return product.getStock() / product.getAverageDailyDemand();
    }

    public int reorderQuantity(Product product) {
        int targetStock = Math.max(product.getReorderPoint() * 2, product.getAverageDailyDemand() * 30);
        return Math.max(0, targetStock - product.getStock());
    }

    public String status(Product product, int daysOfCover) {
        if (product.getStock() < product.getReorderPoint()) {
            return "Kritisch";
        }
        if (daysOfCover <= 14) {
            return "Nachbestellen";
        }
        if (daysOfCover <= 30) {
            return "Beobachten";
        }
        return "Stabil";
    }

    private Supplier findSupplier(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return suppliers.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Lieferant wurde nicht gefunden."));
    }

    private MovementResponse toMovementResponse(InventoryMovement movement) {
        Product product = movement.getProduct();
        return new MovementResponse(
                movement.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                movement.getQuantityChange(),
                movement.getReason(),
                movement.getCreatedAt().toString()
        );
    }

    private String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }
}
