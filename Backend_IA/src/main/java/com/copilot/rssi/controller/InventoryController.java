package com.copilot.rssi.controller;

import com.copilot.rssi.dto.response.ApiResponse;
import com.copilot.rssi.dto.response.DocumentResponse;
import com.copilot.rssi.dto.response.InventoryScanResponse;
import com.copilot.rssi.entity.AnalysisResult;
import com.copilot.rssi.entity.Document;
import com.copilot.rssi.entity.Recommendation;
import com.copilot.rssi.entity.Risk;
import com.copilot.rssi.entity.enums.RiskStatus;
import com.copilot.rssi.repository.AnalysisResultRepository;
import com.copilot.rssi.repository.DocumentRepository;
import com.copilot.rssi.repository.RecommendationRepository;
import com.copilot.rssi.repository.RiskRepository;
import com.copilot.rssi.repository.SensitiveDataRepository;
import com.copilot.rssi.service.interfaces.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventaire")
public class InventoryController {

    private final InventoryService inventoryService;
    private final DocumentRepository documentRepository;
    private final SensitiveDataRepository sensitiveDataRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final RiskRepository riskRepository;
    private final RecommendationRepository recommendationRepository;

    @Value("${app.storage.roots:${user.dir}/sample-documents,${user.dir}/../sample-documents,${user.dir}/uploads,${user.dir}/../uploads}")
    private String storageRoots;

    @PostMapping("/scan")
    @Operation(summary = "Scanner un dossier et inventorier les fichiers")
    public ApiResponse<InventoryScanResponse> triggerScan(@RequestBody Map<String, String> request) {
        String path = request.getOrDefault("directoryPath", request.get("path"));
        if (path == null || path.isBlank()) {
            return ApiResponse.error("Le chemin du dossier est obligatoire.");
        }
        // scanFolder est @Transactional — il commit avant de retourner.
        // L'analyse IA est lancée APRÈS le commit pour éviter le deadlock MySQL (Lock wait timeout).
        InventoryScanResponse result = inventoryService.scanFolder(path.trim());

        List<Long> docIds = result.getDocuments() != null
                ? result.getDocuments().stream()
                    .map(d -> d.getId())
                    .filter(id -> id != null)
                    .toList()
                : List.of();
        if (!docIds.isEmpty()) {
            new Thread(() -> {
                for (Long id : docIds) {
                    try {
                        inventoryService.analyzeDocument(id);
                    } catch (Exception e) {
                        System.err.println("Analyse IA ignorée pour document #" + id + ": " + e.getMessage());
                    }
                }
            }, "ai-scan-thread").start();
        }

        return ApiResponse.ok("Scan terminé avec succès.", result);
    }

    @GetMapping("/documents")
    @Operation(summary = "Lister tous les documents inventoriés")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> getDocuments() {
        List<Object[]> documents = documentRepository.findAllNative();
        List<Map<String, Object>> result = documents.stream()
            .map(row -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", row[0]);
                map.put("fileName", row[1]);
                map.put("filePath", row[2]);
                map.put("extension", row[3]);
                map.put("fileSize", row[4]);
                map.put("mimeType", row[5]);
                map.put("createdDate", row[6]);
                map.put("modifiedDate", row[7]);
                map.put("documentType", row[8]);
                map.put("confidentialityLevel", row[9]);
                map.put("analysisStatus", row[10]);
                map.put("createdAt", row[11]);
                return map;
            })
            .collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    @GetMapping("/sensitive-data")
    @Operation(summary = "Lister toutes les données sensibles détectées")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> getSensitiveData() {
        List<Object[]> sensitiveDataList = sensitiveDataRepository.findAllNative();
        List<Map<String, Object>> result = sensitiveDataList.stream()
            .map(row -> {
                Map<String, Object> map = new java.util.HashMap<>();
                Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
                Long documentId = row[1] != null ? ((Number) row[1]).longValue() : null;
                String fileName = row[2] != null ? row[2].toString() : null;
                String dataType = row[3] != null ? row[3].toString() : "";
                Double confidence = row[6] != null ? ((Number) row[6]).doubleValue() : null;

                map.put("id", id);
                map.put("documentId", documentId);
                map.put("fileName", fileName);
                map.put("dataType", dataType);
                map.put("detectedValue", row[4]);
                map.put("maskedValue", row[5]);
                map.put("confidence", confidence);
                map.put("detectionDate", row[7] != null ? row[7].toString() : null);

                Map<String, Object> document = new java.util.HashMap<>();
                document.put("id", documentId);
                document.put("fileName", fileName);
                map.put("document", document);

                map.put("riskLevel", resolveRiskLevel(dataType, confidence));
                map.put("referential", resolveReferential(documentId, dataType));
                map.put("recommendation", resolveRecommendation(documentId, dataType));
                map.put("treatmentState", resolveTreatmentState(documentId));
                return map;
            })
            .collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    private String resolveRiskLevel(String dataType, Double confidence) {
        double conf = confidence != null ? confidence : 0.0;
        return switch (dataType) {
            case "CIN", "IBAN", "RIB", "BANQUE", "MOT_DE_PASSE", "FISCAL" ->
                    conf >= 0.85 ? "Critique" : "Élevé";
            case "PASSEPORT", "CNSS", "ADRESSE" ->
                    conf >= 0.85 ? "Élevé" : "Moyen";
            case "EMAIL", "TELEPHONE" -> "Moyen";
            default -> conf >= 0.90 ? "Moyen" : "Faible";
        };
    }

    private String resolveReferential(Long documentId, String dataType) {
        if (documentId != null) {
            Optional<AnalysisResult> analysisResult =
                    analysisResultRepository.findFirstByDocumentIdOrderByCreatedAtDesc(documentId);
            if (analysisResult.isPresent()) {
                String standards = analysisResult.get().getComplianceStandards();
                if (standards != null && !standards.isBlank()) {
                    return Arrays.stream(standards.split(","))
                            .map(String::trim)
                            .filter(value -> !value.isBlank())
                            .findFirst()
                            .orElse(mapReferentialByDataType(dataType));
                }
            }
        }
        return mapReferentialByDataType(dataType);
    }

    private String mapReferentialByDataType(String dataType) {
        return switch (dataType) {
            case "CIN", "PASSEPORT", "CNSS", "EMAIL", "TELEPHONE", "ADRESSE" -> "Loi 09-08";
            case "IBAN", "RIB", "BANQUE", "FISCAL" -> "ISO 27001";
            case "MOT_DE_PASSE" -> "CIS Controls";
            default -> "ISO 27001";
        };
    }

    private String resolveRecommendation(Long documentId, String dataType) {
        if (documentId != null) {
            List<Risk> risks = riskRepository.findByDocumentId(documentId);
            for (Risk risk : risks) {
                List<Recommendation> recommendations = recommendationRepository.findByRiskId(risk.getId());
                if (!recommendations.isEmpty()) {
                    String description = recommendations.get(0).getDescription();
                    if (description != null && !description.isBlank()) {
                        return description;
                    }
                }
            }
        }
        return mapRecommendationByDataType(dataType);
    }

    private String mapRecommendationByDataType(String dataType) {
        return switch (dataType) {
            case "CIN" -> "Masquer partiellement, restreindre l'accès";
            case "IBAN", "RIB", "BANQUE" -> "Chiffrer, restreindre l'accès";
            case "EMAIL" -> "Masquer, restreindre l'accès";
            case "TELEPHONE" -> "Masquer partiellement";
            case "MOT_DE_PASSE" -> "Supprimer immédiatement, forcer changement";
            default -> "Masquer ou restreindre l'accès";
        };
    }

    private String resolveTreatmentState(Long documentId) {
        if (documentId == null) {
            return "À traiter";
        }
        List<Risk> risks = riskRepository.findByDocumentId(documentId);
        if (risks.isEmpty()) {
            return "À traiter";
        }
        boolean hasOpen = risks.stream().anyMatch(risk -> risk.getStatus() == RiskStatus.OUVERT);
        boolean hasInProgress = risks.stream().anyMatch(risk -> risk.getStatus() == RiskStatus.EN_COURS);
        if (hasOpen) {
            return "À traiter";
        }
        if (hasInProgress) {
            return "En cours";
        }
        return "Traité";
    }

    @PostMapping("/documents/{id}/analyze")
    @Operation(summary = "Analyser un document avec l'IA")
    public ApiResponse<DocumentResponse> analyzeDocument(@PathVariable Long id) {
        try {
            return ApiResponse.ok("Analyse terminée.", inventoryService.analyzeDocument(id));
        } catch (Exception e) {
            return ApiResponse.error("Erreur lors de l'analyse: " + e.getMessage());
        }
    }

    @DeleteMapping("/documents/{id}")
    @Operation(summary = "Supprimer un document")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        inventoryService.deleteDocument(id);
        return ApiResponse.ok("Document supprimé avec succès.", null);
    }

    @GetMapping("/documents/{id}/preview")
    @Operation(summary = "Prévisualiser un document")
    public ResponseEntity<byte[]> previewDocument(@PathVariable Long id) throws IOException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable: " + id));
        return serveDocument(document, false);
    }

    @GetMapping("/documents/{id}/download")
    @Operation(summary = "Télécharger un document")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) throws IOException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable: " + id));
        return serveDocument(document, true);
    }

    private ResponseEntity<byte[]> serveDocument(Document document, boolean download) throws IOException {
        if (document.getFilePath() == null || document.getFilePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Path path = resolveDocumentPath(document);
        if (path == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        String filename = document.getFileName() != null ? document.getFileName() : "document";
        headers.add(HttpHeaders.CONTENT_DISPOSITION, (download ? "attachment" : "inline") + "; filename=\"" + filename + "\"");
        return ResponseEntity.ok().headers(headers).body(Files.readAllBytes(path));
    }

    /**
     * Old inventory rows contain absolute paths from another machine/project folder.
     * Prefer that path, then locate the same filename under the configured document roots.
     */
    private Path resolveDocumentPath(Document document) {
        if (document.getFilePath() == null || document.getFilePath().isBlank()) {
            return null;
        }

        Path storedPath = Path.of(document.getFilePath()).normalize();
        if (Files.isRegularFile(storedPath)) {
            return storedPath;
        }

        String fileName = document.getFileName();
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        return Arrays.stream(storageRoots.split(","))
                .map(String::trim)
                .filter(root -> !root.isBlank())
                .map(root -> Path.of(root).toAbsolutePath().normalize())
                .filter(Files::isDirectory)
                .map(root -> findFile(root, fileName))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(null);
    }

    private Optional<Path> findFile(Path root, String fileName) {
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(fileName))
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
