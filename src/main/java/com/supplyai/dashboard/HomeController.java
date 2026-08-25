package com.supplyai.dashboard;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HomeController {

    private final DashboardService dashboardService;

    HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/status")
    Map<String, String> home() {
        return Map.of(
                "application", "SupplyAI",
                "message", "Willkommen bei SupplyAI",
                "status", "Die Anwendung läuft"
        );
    }

    @GetMapping("/api/dashboard")
    DashboardService.DashboardOverview dashboard() {
        return dashboardService.overview();
    }
}
