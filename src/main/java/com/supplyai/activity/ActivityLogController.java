package com.supplyai.activity;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
class ActivityLogController {

    private final ActivityLogService activities;

    ActivityLogController(ActivityLogService activities) {
        this.activities = activities;
    }

    @GetMapping
    List<ActivityLogService.ActivityResponse> recent() {
        return activities.recent();
    }
}
