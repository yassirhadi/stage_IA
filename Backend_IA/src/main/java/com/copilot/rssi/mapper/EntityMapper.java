package com.copilot.rssi.mapper;

import com.copilot.rssi.entity.*;
import com.copilot.rssi.dto.response.*;
import com.copilot.rssi.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EntityMapper {

    private final RoleRepository roleRepository;

    // Asset -> AssetResponse
    public AssetResponse toAssetResponse(Asset asset) {
        if (asset == null) return null;
        return AssetResponse.builder()
                .id(asset.getId())
                .name(asset.getName())
                .assetType(asset.getAssetType())
                .description(asset.getDescription())
                .owner(asset.getOwner())
                .criticality(asset.getCriticality())
                .status(asset.getStatus())
                .analysisStatus(asset.getAnalysisStatus())
                .confidentialityLevel(asset.getConfidentialityLevel())
                .extension(asset.getExtension())
                .path(asset.getPath())
                .size(asset.getSize())
                .folderId(asset.getFolderId())
                .value(asset.getValue())
                .creationDate(asset.getCreationDate())
                .location(asset.getLocation())
                .responsible(asset.getResponsible())
                .state(asset.getState())
                .createdAt(asset.getCreatedAt())
                .build();
    }

    // Folder -> FolderResponse
    public FolderResponse toFolderResponse(Folder folder) {
        if (folder == null) return null;
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .path(folder.getPath())
                .scannedAt(folder.getScannedAt() != null ? folder.getScannedAt() : folder.getCreatedAt())
                .build();
    }

    // Report -> ReportResponse
    public ReportResponse toReportResponse(Report report) {
        if (report == null) return null;
        return ReportResponse.builder()
                .id(report.getId())
                .title(report.getTitle())
                .reportType(report.getReportType())
                .content(report.getContent())
                .executiveSummary(report.getExecutiveSummary())
                .overallScore(report.getOverallScore())
                .rssiSignature(report.getRssiSignature())
                .generatedBy(report.getGeneratedBy() != null ? report.getGeneratedBy().getUsername() : null)
                .generatedAt(report.getGeneratedAt())
                .signatureHash(null) // Not in entity, can be added if needed
                .build();
    }

    // User -> UserResponse
    public UserResponse toUserResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // Recommendation -> RecommendationResponse
    public RecommendationResponse toRecommendationResponse(Recommendation recommendation) {
        if (recommendation == null) return null;
        return RecommendationResponse.builder()
                .id(recommendation.getId())
                .riskId(recommendation.getRisk() != null ? recommendation.getRisk().getId() : null)
                .riskTitle(recommendation.getRisk() != null ? recommendation.getRisk().getTitle() : null)
                .description(recommendation.getDescription())
                .priority(recommendation.getPriority())
                .status(recommendation.getStatus())
                .deadline(recommendation.getDeadline())
                .progress(recommendation.getProgress())
                .rssiComment(recommendation.getRssiComment())
                .createdAt(recommendation.getCreatedAt())
                .updatedAt(recommendation.getUpdatedAt())
                .build();
    }

    // Referential -> ReferentialResponse
    public ReferentialResponse toReferentialResponse(Referential referential) {
        if (referential == null) return null;
        return ReferentialResponse.builder()
                .id(referential.getId())
                .code(referential.getCode())
                .name(referential.getName())
                .category(referential.getCategory())
                .content(referential.getContent())
                .version(referential.getVersion())
                .active(referential.getActive())
                .createdAt(referential.getCreatedAt())
                .build();
    }

    // AuditLog -> AuditLogResponse
    public AuditLogResponse toAuditLogResponse(AuditLog auditLog) {
        if (auditLog == null) return null;
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .username(auditLog.getUser() != null ? auditLog.getUser().getUsername() : null)
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .details(auditLog.getDetails())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    // Document -> DocumentResponse
    public DocumentResponse toDocumentResponse(Document document) {
        if (document == null) return null;
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .filePath(document.getFilePath())
                .extension(document.getExtension())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .createdDate(document.getCreatedDate())
                .modifiedDate(document.getModifiedDate())
                .permissions(document.getPermissions())
                .documentType(document.getDocumentType())
                .confidentialityLevel(document.getConfidentialityLevel())
                .analysisStatus(document.getAnalysisStatus())
                .createdAt(document.getCreatedAt())
                .build();
    }

    // Document -> DocumentResponse with analysis data
    public DocumentResponse toDocumentResponseWithAnalysis(Document document, AnalysisResult analysisResult) {
        if (document == null) return null;
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .filePath(document.getFilePath())
                .extension(document.getExtension())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .createdDate(document.getCreatedDate())
                .modifiedDate(document.getModifiedDate())
                .permissions(document.getPermissions())
                .documentType(document.getDocumentType())
                .confidentialityLevel(document.getConfidentialityLevel())
                .analysisStatus(document.getAnalysisStatus())
                .createdAt(document.getCreatedAt())
                .securityScore(analysisResult != null ? analysisResult.getSecurityScore() : null)
                .piiDetected(analysisResult != null ? analysisResult.getPiiDetected() : null)
                .detectedDataTypes(analysisResult != null ? analysisResult.getDetectedDataTypes() : null)
                .risksCount(analysisResult != null ? analysisResult.getRisksCount() : null)
                .risksDetails(analysisResult != null ? analysisResult.getRisksDetails() : null)
                .complianceStandards(analysisResult != null ? analysisResult.getComplianceStandards() : null)
                .build();
    }

    public AnalysisResultResponse toAnalysisResultResponse(AnalysisResult analysisResult) {
        if (analysisResult == null) return null;
        return AnalysisResultResponse.builder()
                .id(analysisResult.getId())
                .documentId(analysisResult.getDocumentId())
                .documentType(analysisResult.getDocumentType())
                .confidentialityLevel(analysisResult.getConfidentialityLevel())
                .piiDetected(analysisResult.getPiiDetected())
                .risksCount(analysisResult.getRisksCount())
                .complianceStandards(analysisResult.getComplianceStandards())
                .securityScore(analysisResult.getSecurityScore())
                .detectedDataTypes(analysisResult.getDetectedDataTypes())
                .risksDetails(analysisResult.getRisksDetails())
                .createdAt(analysisResult.getCreatedAt())
                .build();
    }

    // Permission -> PermissionResponse
    public PermissionResponse toPermissionResponse(Permission permission) {
        if (permission == null) return null;

        List<String> roles = roleRepository.findAll().stream()
                .filter(role -> role.getPermissions() != null && role.getPermissions().stream().anyMatch(p -> p.getId() != null && p.getId().equals(permission.getId())))
                .map(role -> role.getName() != null ? role.getName().name() : null)
                .filter(name -> name != null)
                .sorted()
                .toList();

        String category = resolvePermissionCategory(permission.getName());
        String module = resolvePermissionModule(permission.getName());

        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .category(category)
                .module(module)
                .roles(roles)
                .roleCount(roles.size())
                .isCritical(isCriticalPermission(permission.getName()))
                .createdAt(permission.getCreatedAt())
                .build();
    }

    private String resolvePermissionCategory(String permissionName) {
        if (permissionName == null) return "Autre";
        if (permissionName.contains("user")) return "Utilisateurs";
        if (permissionName.contains("document")) return "Documents";
        if (permissionName.contains("report")) return "Rapports";
        if (permissionName.contains("risk")) return "Risques";
        if (permissionName.contains("setting") || permissionName.contains("permission") || permissionName.contains("role")) return "Administration";
        if (permissionName.contains("backup")) return "Sauvegardes";
        if (permissionName.contains("inventory")) return "Inventaire";
        if (permissionName.contains("sensitive")) return "Données sensibles";
        return "Autre";
    }

    private String resolvePermissionModule(String permissionName) {
        if (permissionName == null) return "Autre";
        if (permissionName.contains("user")) return "Users";
        if (permissionName.contains("document")) return "Documents";
        if (permissionName.contains("report")) return "Reports";
        if (permissionName.contains("risk")) return "Risks";
        if (permissionName.contains("setting")) return "Settings";
        if (permissionName.contains("permission")) return "Admin";
        if (permissionName.contains("role")) return "Admin";
        if (permissionName.contains("backup")) return "Backups";
        if (permissionName.contains("inventory")) return "Inventory";
        if (permissionName.contains("sensitive")) return "SensitiveData";
        return "Autre";
    }

    private boolean isCriticalPermission(String permissionName) {
        if (permissionName == null) return false;
        return permissionName.contains("delete") || permissionName.contains("create") || permissionName.contains("manage") || permissionName.contains("edit");
    }

    // Role -> RoleResponse
    public RoleResponse toRoleResponse(Role role) {
        if (role == null) return null;
        Set<PermissionResponse> permissions = role.getPermissions() != null ?
                role.getPermissions().stream().map(this::toPermissionResponse).collect(Collectors.toSet()) :
                Set.of();
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permissions)
                .createdAt(role.getCreatedAt())
                .build();
    }

    // Setting -> SettingResponse
    public SettingResponse toSettingResponse(Setting setting) {
        if (setting == null) return null;
        return SettingResponse.builder()
                .id(setting.getId())
                .companyName(setting.getCompanyName())
                .logoUrl(setting.getLogoUrl())
                .language(setting.getLanguage())
                .timezone(setting.getTimezone())
                .email(setting.getEmail())
                .storagePath(setting.getStoragePath())
                .ocrEnabled(setting.getOcrEnabled())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}