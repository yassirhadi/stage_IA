package com.copilot.rssi.dto.response;

import com.copilot.rssi.entity.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponse {
    private Long id;
    private String fileName;
    private String filePath;
    private String extension;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String permissions;
    private DocumentType documentType;
    private ConfidentialityLevel confidentialityLevel;
    private AnalysisStatus analysisStatus;
    private LocalDateTime createdAt;
    private Integer securityScore;
    private Boolean piiDetected;
    private String detectedDataTypes;
    private Integer risksCount;
    private String risksDetails;
    private String complianceStandards;
}
