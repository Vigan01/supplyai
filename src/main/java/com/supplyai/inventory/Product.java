package com.supplyai.inventory;

import java.math.BigDecimal;
import java.time.Instant;

import com.supplyai.supplier.Supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String sku;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String category;

    @Min(0)
    @Column(nullable = false)
    private int stock;

    @Min(0)
    @Column(nullable = false)
    private int reorderPoint;

    @Min(0)
    @Column(nullable = false)
    private int averageDailyDemand;

    @NotNull
    @DecimalMin("0.00")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    public Product(String sku, String name, String category, int stock, int reorderPoint,
            int averageDailyDemand, BigDecimal unitCost, Supplier supplier) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.stock = stock;
        this.reorderPoint = reorderPoint;
        this.averageDailyDemand = averageDailyDemand;
        this.unitCost = unitCost;
        this.supplier = supplier;
    }

    @PrePersist
    @PreUpdate
    void markUpdated() {
        updatedAt = Instant.now();
    }

    public void update(String sku, String name, String category, int stock, int reorderPoint,
            int averageDailyDemand, BigDecimal unitCost, Supplier supplier) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.stock = stock;
        this.reorderPoint = reorderPoint;
        this.averageDailyDemand = averageDailyDemand;
        this.unitCost = unitCost;
        this.supplier = supplier;
    }

    public void applyMovement(int quantityChange) {
        int nextStock = stock + quantityChange;
        if (nextStock < 0) {
            throw new IllegalArgumentException("Der Bestand darf nicht negativ werden.");
        }
        stock = nextStock;
    }

    public void clearSupplier() {
        supplier = null;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public int getStock() {
        return stock;
    }

    public int getReorderPoint() {
        return reorderPoint;
    }

    public int getAverageDailyDemand() {
        return averageDailyDemand;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
