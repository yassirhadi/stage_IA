package com.copilot.rssi.dto.response;

import com.copilot.rssi.entity.enums.RecommendationPriority;
import com.copilot.rssi.entity.enums.RecommendationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@Builder
public class RecommendationResponse {
    private Long id;
    private Long riskId;
    private String riskTitle;
    private String description;
    private RecommendationPriority priority;
    private RecommendationStatus status;
    private LocalDate deadline;
    private Integer progress;
    private String rssiComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
