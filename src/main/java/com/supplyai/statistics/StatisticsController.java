package com.supplyai.statistics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
class StatisticsController {

    private final StatisticsService statistics;

    StatisticsController(StatisticsService statistics) {
        this.statistics = statistics;
    }

    @GetMapping
    StatisticsService.StatisticsOverview overview() {
        return statistics.overview();
    }
}
