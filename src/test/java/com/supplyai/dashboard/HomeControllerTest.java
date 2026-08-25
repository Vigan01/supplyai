package com.supplyai.dashboard;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supplyai.inventory.ProductRepository;
import com.supplyai.purchase.PurchaseOrderRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:supplyai-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository products;

    @Autowired
    private PurchaseOrderRepository purchaseOrders;

    @Test
    void returnsGermanWelcomeMessage() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application", is("SupplyAI")))
                .andExpect(jsonPath("$.message", is("Willkommen bei SupplyAI")))
                .andExpect(jsonPath("$.status", is("Die Anwendung läuft")));
    }

    @Test
    void returnsDashboardOverview() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics[0].label", is("Aktive Produkte")))
                .andExpect(jsonPath("$.inventory[0].sku", is("SKU-1048")))
                .andExpect(jsonPath("$.alerts[0].severity", notNullValue()));
    }

    @Test
    void listsSuppliers() throws Exception {
        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", notNullValue()));
    }

    @Test
    void createsSupplier() throws Exception {
        String supplier = """
                {
                  "name": "Quality Supply Test",
                  "email": "quality@example.com",
                  "leadTimeDays": 7,
                  "delayed": false
                }
                """;

        mockMvc.perform(post("/api/suppliers")
                        .contentType("application/json")
                        .content(supplier))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Quality Supply Test")));
    }

    @Test
    void createsProductAndBooksMovement() throws Exception {
        String product = """
                {
                  "sku": "SKU-TEST-1",
                  "name": "Testprodukt",
                  "category": "Test",
                  "stock": 12,
                  "reorderPoint": 10,
                  "averageDailyDemand": 2,
                  "unitCost": 4.50,
                  "supplierId": null
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(product))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku", is("SKU-TEST-1")))
                .andExpect(jsonPath("$.status", is("Nachbestellen")));

        String movement = """
                {
                  "type": "IN",
                  "quantity": 8,
                  "reason": "Testlieferung"
                }
                """;

        Long productId = products.findBySku("SKU-TEST-1").orElseThrow().getId();

        mockMvc.perform(post("/api/products/" + productId + "/movements")
                        .contentType("application/json")
                        .content(movement))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantityChange", is(8)));

        mockMvc.perform(get("/api/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type", is("MOVEMENT")));
    }

    @Test
    void exportsProductsAsCsv() throws Exception {
        mockMvc.perform(get("/api/products/csv"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sku,name,category")));
    }

    @Test
    void returnsStatisticsOverview() throws Exception {
        mockMvc.perform(get("/api/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[0].label", is("Lagerwert")))
                .andExpect(jsonPath("$.statusDistribution[0].label", notNullValue()))
                .andExpect(jsonPath("$.supplierRisk.totalSuppliers", notNullValue()));
    }

    @Test
    void createsPurchaseOrderAndUpdatesStatus() throws Exception {
        Long productId = products.findBySku("SKU-1048").orElseThrow().getId();
        String order = """
                {
                  "productId": %d,
                  "supplierId": null,
                  "quantity": 25,
                  "expectedDeliveryDate": "2026-09-10",
                  "note": "Integrationstest"
                }
                """.formatted(productId);

        mockMvc.perform(post("/api/purchase-orders")
                        .contentType("application/json")
                        .content(order))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PLANNED")))
                .andExpect(jsonPath("$.quantity", is(25)));

        mockMvc.perform(get("/api/purchase-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status", is("PLANNED")));

        Long orderId = purchaseOrders.findAllByOrderByCreatedAtDesc().getFirst().getId();

        String statusUpdate = """
                {
                  "status": "ORDERED"
                }
                """;

        mockMvc.perform(patch("/api/purchase-orders/" + orderId + "/status")
                        .contentType("application/json")
                        .content(statusUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ORDERED")));
    }
}
