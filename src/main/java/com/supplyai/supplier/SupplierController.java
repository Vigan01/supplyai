package com.supplyai.supplier;

import static com.supplyai.supplier.SupplierDtos.SupplierRequest;
import static com.supplyai.supplier.SupplierDtos.SupplierResponse;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suppliers")
class SupplierController {

    private final SupplierService suppliers;

    SupplierController(SupplierService suppliers) {
        this.suppliers = suppliers;
    }

    @GetMapping
    List<SupplierResponse> listSuppliers() {
        return suppliers.listSuppliers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SupplierResponse createSupplier(@Valid @RequestBody SupplierRequest request) {
        return suppliers.createSupplier(request);
    }

    @PutMapping("/{id}")
    SupplierResponse updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return suppliers.updateSupplier(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteSupplier(@PathVariable Long id) {
        suppliers.deleteSupplier(id);
    }
}
