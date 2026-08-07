package com.copilot.rssi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupResponse {
    private Long id;
    private String name;
    private String date;
    private String size;
    private Long sizeBytes;
    private String status;
    private String type;
    private String schedule;
    private String lastBackup;
    private String nextBackup;
    private String description;
}
