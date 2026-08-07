package com.copilot.rssi.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PermissionResponse {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String module;
    private List<String> roles;
    private Integer roleCount;
    private Boolean isCritical;
    private LocalDateTime createdAt;
}
