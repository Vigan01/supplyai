package com.supplyai.activity;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {

    private final ActivityLogRepository logs;

    ActivityLogService(ActivityLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String type, String entityType, Long entityId, String title, String details) {
        logs.save(new ActivityLog(type, entityType, entityId, title, details));
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> recent() {
        return logs.findTop50ByOrderByCreatedAtDesc().stream()
                .map(log -> new ActivityResponse(
                        log.getId(),
                        log.getType(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getTitle(),
                        log.getDetails(),
                        log.getCreatedAt().toString()
                ))
                .toList();
    }

    public record ActivityResponse(
            Long id,
            String type,
            String entityType,
            Long entityId,
            String title,
            String details,
            String createdAt) {
    }
}
