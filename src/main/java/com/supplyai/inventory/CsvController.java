package com.supplyai.inventory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products/csv")
class CsvController {

    private final CsvService csvService;

    CsvController(CsvService csvService) {
        this.csvService = csvService;
    }

    @GetMapping
    ResponseEntity<String> exportProducts() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=supplyai-products.csv")
                .contentType(new MediaType("text", "csv"))
                .body(csvService.exportProducts());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    CsvService.ImportSummary importProducts(@RequestParam("file") MultipartFile file) {
        return csvService.importProducts(file);
    }
}
