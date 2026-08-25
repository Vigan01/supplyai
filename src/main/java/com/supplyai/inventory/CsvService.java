package com.supplyai.inventory;

import static com.supplyai.inventory.ProductDtos.ProductRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.supplyai.activity.ActivityLogService;
import com.supplyai.supplier.Supplier;
import com.supplyai.supplier.SupplierRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CsvService {

    private final ProductService productService;
    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final ActivityLogService activities;

    CsvService(ProductService productService, ProductRepository products, SupplierRepository suppliers,
            ActivityLogService activities) {
        this.productService = productService;
        this.products = products;
        this.suppliers = suppliers;
        this.activities = activities;
    }

    @Transactional(readOnly = true)
    public String exportProducts() {
        StringBuilder csv = new StringBuilder("sku,name,category,stock,reorderPoint,averageDailyDemand,unitCost,supplierName\n");
        for (Product product : products.findAll()) {
            csv.append(escape(product.getSku())).append(',')
                    .append(escape(product.getName())).append(',')
                    .append(escape(product.getCategory())).append(',')
                    .append(product.getStock()).append(',')
                    .append(product.getReorderPoint()).append(',')
                    .append(product.getAverageDailyDemand()).append(',')
                    .append(product.getUnitCost()).append(',')
                    .append(escape(product.getSupplier() == null ? "" : product.getSupplier().getName()))
                    .append('\n');
        }
        return csv.toString();
    }

    @Transactional
    public ImportSummary importProducts(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<String> lines = content.lines()
                    .filter(line -> !line.isBlank())
                    .toList();
            int imported = 0;
            List<String> errors = new ArrayList<>();

            for (int index = 1; index < lines.size(); index++) {
                String line = lines.get(index);
                try {
                    List<String> columns = parseLine(line);
                    if (columns.size() < 8) {
                        throw new IllegalArgumentException("Erwartet 8 Spalten.");
                    }
                    Supplier supplier = suppliers.findByName(columns.get(7).trim()).orElse(null);
                    ProductRequest request = new ProductRequest(
                            columns.get(0).trim(),
                            columns.get(1).trim(),
                            columns.get(2).trim(),
                            Integer.parseInt(columns.get(3).trim()),
                            Integer.parseInt(columns.get(4).trim()),
                            Integer.parseInt(columns.get(5).trim()),
                            new BigDecimal(columns.get(6).trim()),
                            supplier == null ? null : supplier.getId()
                    );
                    products.findBySku(request.sku())
                            .ifPresentOrElse(
                                    product -> productService.updateProduct(product.getId(), request),
                                    () -> productService.createProduct(request)
                            );
                    imported++;
                } catch (RuntimeException exception) {
                    errors.add("Zeile " + (index + 1) + ": " + exception.getMessage());
                }
            }
            activities.record("IMPORT", "CSV", null, "CSV importiert",
                    imported + " Produktzeilen importiert, " + errors.size() + " Fehler.");
            return new ImportSummary(imported, errors);
        } catch (Exception exception) {
            throw new IllegalArgumentException("CSV konnte nicht gelesen werden.");
        }
    }

    private String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private List<String> parseLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char value = line.charAt(index);
            if (value == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (value == ',' && !quoted) {
                columns.add(current.toString());
                current.setLength(0);
            } else {
                current.append(value);
            }
        }
        columns.add(current.toString());
        return columns;
    }

    public record ImportSummary(int imported, List<String> errors) {
    }
}
