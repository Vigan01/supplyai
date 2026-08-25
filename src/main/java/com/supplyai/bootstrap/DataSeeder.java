package com.supplyai.bootstrap;

import java.math.BigDecimal;
import java.util.List;

import com.supplyai.inventory.Product;
import com.supplyai.inventory.ProductRepository;
import com.supplyai.supplier.Supplier;
import com.supplyai.supplier.SupplierRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class DataSeeder implements CommandLineRunner {

    private final ProductRepository products;
    private final SupplierRepository suppliers;

    DataSeeder(ProductRepository products, SupplierRepository suppliers) {
        this.products = products;
        this.suppliers = suppliers;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (products.count() > 0) {
            return;
        }

        Supplier techParts = suppliers.save(new Supplier("TechParts GmbH", "orders@techparts.example", 9, false));
        Supplier metalWorks = suppliers.save(new Supplier("MetalWorks AG", "supply@metalworks.example", 14, false));
        Supplier packline = suppliers.save(new Supplier("Packline Europe", "ops@packline.example", 6, true));

        products.saveAll(List.of(
                new Product("SKU-1048", "Industriesensor M12", "Elektronik", 18, 72, 6, new BigDecimal("42.90"), techParts),
                new Product("SKU-2210", "Aluminiumprofil 40x40", "Rohmaterial", 214, 160, 5, new BigDecimal("18.40"), metalWorks),
                new Product("SKU-3092", "Verpackungseinheit S", "Verpackung", 46, 80, 9, new BigDecimal("0.82"), packline),
                new Product("SKU-4471", "Dichtung EPDM", "Komponente", 390, 240, 8, new BigDecimal("1.35"), techParts),
                new Product("SKU-5124", "Steuerplatine V2", "Elektronik", 31, 54, 2, new BigDecimal("96.00"), techParts)
        ));
    }
}
