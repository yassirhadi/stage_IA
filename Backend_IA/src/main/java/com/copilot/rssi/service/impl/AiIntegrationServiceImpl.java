package com.copilot.rssi.service.impl;

import com.copilot.rssi.dto.request.ChatRequest;
import com.copilot.rssi.dto.response.RiskResponse;
import com.copilot.rssi.entity.AnalysisResult;
import com.copilot.rssi.entity.Document;
import com.copilot.rssi.entity.Recommendation;
import com.copilot.rssi.entity.Risk;
import com.copilot.rssi.entity.SensitiveData;
import com.copilot.rssi.entity.enums.*;
import com.copilot.rssi.repository.AnalysisResultRepository;
import com.copilot.rssi.repository.DocumentRepository;
import com.copilot.rssi.repository.RecommendationRepository;
import com.copilot.rssi.repository.RiskRepository;
import com.copilot.rssi.repository.SensitiveDataRepository;
import com.copilot.rssi.service.interfaces.AiIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiIntegrationServiceImpl implements AiIntegrationService {

    private final DocumentRepository documentRepository;
    private final SensitiveDataRepository sensitiveDataRepository;
    private final RiskRepository riskRepository;
    private final RecommendationRepository recommendationRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ai-service.base-url:http://localhost:8000}")
    private String aiBaseUrl;

    @Value("${app.storage.roots:${user.dir}/sample-documents,${user.dir}/../sample-documents,${user.dir}/uploads,${user.dir}/../uploads}")
    private String storageRoots;

    @Transactional
    public void analyzeAndPersist(Document document) {
        document.setAnalysisStatus(AnalysisStatus.IN_PROGRESS);
        documentRepository.save(document);

        try {
            Path actualPath = resolveDocumentPath(document);
            if (actualPath == null) {
                throw new IllegalStateException("Fichier introuvable pour le document: " + document.getFileName());
            }
            Map<String, Object> result = callAnalyzeApi(actualPath.toString());
            if (result.containsKey("error")) {
                log.error("Service IA indisponible pour {}: {}", document.getFileName(), result.get("error"));
                document.setAnalysisStatus(AnalysisStatus.FAILED);
                documentRepository.save(document);
                throw new IllegalStateException("Service IA indisponible: " + result.get("error"));
            }
            applyAnalysisResult(document, result);
        } catch (Exception e) {
            log.error("Analysis failed for {}: {}", document.getFileName(), e.getMessage(), e);
            document.setAnalysisStatus(AnalysisStatus.FAILED);
            documentRepository.save(document);
            throw new IllegalStateException("Analyse échouée: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> analyzeDocument(Long documentId, String filePath) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable"));
        analyzeAndPersist(document);
        Map<String, Object> result = new HashMap<>();
        result.put("documentId", document.getId());
        result.put("analysisStatus", document.getAnalysisStatus().name());
        result.put("documentType", document.getDocumentType());
        result.put("confidentialityLevel", document.getConfidentialityLevel());
        return result;
    }

    @Override
    public Map<String, Object> chat(ChatRequest request) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> payload = new HashMap<>();
            payload.put("question", request.getQuestion());

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiBaseUrl + "/api/v1/chat", requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String answer = body.get("answer") != null
                        ? body.get("answer").toString()
                        : (body.get("reply") != null ? body.get("reply").toString() : "Pas de réponse.");
                resultMap.put("answer", answer);
                if (body.get("sources") != null) {
                    resultMap.put("sources", body.get("sources"));
                }
            } else {
                resultMap.put("answer", "Erreur de communication avec le Copilote IA.");
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'appel au service IA pour le chat", e);
            resultMap.put("answer", "Service IA indisponible. Démarrez le service Python (port 8000).");
        }
        return resultMap;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiskResponse> getAllRisks() {
        return riskRepository.findAll().stream()
                .map(this::toRiskResponse)
                .toList();
    }

    @SuppressWarnings("unchecked")
    @Transactional
    private void applyAnalysisResult(Document document, Map<String, Object> result) {
        Object extracted = result.get("extracted_text");
        if (extracted == null) {
            extracted = result.get("extractedText");
        }
        if (extracted != null) {
            String text = extracted.toString();
            document.setExtractedText(text.length() > 65000 ? text.substring(0, 65000) : text);
        }

        Object docType = result.get("document_type");
        if (docType == null) {
            docType = result.get("documentType");
        }
        document.setDocumentType(mapDocumentType(docType != null ? docType.toString() : null));

        Object confLevel = result.get("confidentiality_level");
        if (confLevel == null) {
            confLevel = result.get("confidentialityLevel");
        }
        document.setConfidentialityLevel(mapConfidentiality(confLevel != null ? confLevel.toString() : null));
        document.setAnalysisStatus(AnalysisStatus.COMPLETED);
        documentRepository.save(document);

        saveAnalysisResult(document, result);
        // Delete dependent recommendations first so re-analysis respects FK constraints.
        List<Risk> existingRisks = riskRepository.findByDocumentId(document.getId());
        for (Risk existingRisk : existingRisks) {
            recommendationRepository.deleteAll(recommendationRepository.findByRiskId(existingRisk.getId()));
        }
        riskRepository.deleteAll(existingRisks);
        sensitiveDataRepository.deleteByDocumentId(document.getId());
        sensitiveDataRepository.flush();

        List<Map<String, Object>> sensitiveItems = (List<Map<String, Object>>) result.get("sensitive_data");
        if (sensitiveItems == null) {
            sensitiveItems = (List<Map<String, Object>>) result.get("sensitiveData");
        }
        if (sensitiveItems != null) {
            for (Map<String, Object> item : sensitiveItems) {
                saveSensitiveData(document, item);
            }
        }

        List<Map<String, Object>> risks = (List<Map<String, Object>>) result.get("risks");
        List<Map<String, Object>> recommendations = (List<Map<String, Object>>) result.get("recommendations");

        if (risks != null) {
            for (int i = 0; i < risks.size(); i++) {
                Map<String, Object> riskData = risks.get(i);
                Risk risk = saveRisk(document, riskData);
                if (recommendations != null && i < recommendations.size()) {
                    Map<String, Object> recommendationData = recommendations.get(i);
                    if (recommendationData != null && containsRecommendationContent(recommendationData)) {
                        saveRecommendation(risk, recommendationData);
                    }
                }
            }
        }
    }

    private Map<String, Object> callAnalyzeApi(String filePath) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = Map.of("file_path", filePath, "persist", false);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiBaseUrl + "/api/v1/analyze", requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                resultMap.putAll(response.getBody());
            } else {
                // Treat non-OK responses (including 404) as errors
                resultMap.put("error", "Réponse IA invalide: " + response.getStatusCode());
            }
        } catch (Exception e) {
            resultMap.put("error", e.getMessage());
        }
        return resultMap;
    }

    private Path resolveDocumentPath(Document document) {
        if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
            Path storedPath = Path.of(document.getFilePath()).normalize();
            if (Files.isRegularFile(storedPath)) return storedPath;
        }
        if (document.getFileName() == null || document.getFileName().isBlank()) return null;

        return Arrays.stream(storageRoots.split(","))
                .map(String::trim)
                .filter(root -> !root.isBlank())
                .map(root -> Path.of(root).toAbsolutePath().normalize())
                .filter(Files::isDirectory)
                .map(root -> findFile(root, document.getFileName()))
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

    private void saveAnalysisResult(Document document, Map<String, Object> result) {
        List<Map<String, Object>> sensitiveItems = (List<Map<String, Object>>) result.get("sensitive_data");
        if (sensitiveItems == null) {
            sensitiveItems = (List<Map<String, Object>>) result.get("sensitiveData");
        }
        List<Map<String, Object>> risks = (List<Map<String, Object>>) result.get("risks");
        List<Map<String, Object>> recommendations = (List<Map<String, Object>>) result.get("recommendations");

        String detectedDataTypes = sensitiveItems == null ? "" : sensitiveItems.stream()
                .map(item -> item.get("data_type") != null ? item.get("data_type").toString() : item.get("dataType") != null ? item.get("dataType").toString() : "")
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        String risksDetails = risks == null ? "" : risks.stream()
                .map(item -> item.get("title") != null ? item.get("title").toString() : "")
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("\n"));

        Object standardsValue = result.get("compliance_standards");
        if (standardsValue == null) standardsValue = result.get("complianceStandards");
        String complianceStandards = standardsValue instanceof Collection<?> collection
            ? collection.stream().map(Object::toString).collect(Collectors.joining(", "))
            : standardsValue != null ? standardsValue.toString() : "";
        Object scoreValue = result.get("security_score");
        if (scoreValue == null) scoreValue = result.get("securityScore");
        Integer securityScore = scoreValue != null
                ? Integer.valueOf(scoreValue.toString())
                : calculateSecurityScore(result, sensitiveItems, risks);
        
        if (scoreValue == null) {
            log.warn("AI payload missing security score for document {}. Computed fallback score {}.", document.getId(), securityScore);
        }
        
        System.out.println("=== SAVING ANALYSIS RESULT ===");
        System.out.println("Document ID: " + document.getId());
        System.out.println("Security Score: " + securityScore);
        System.out.println("Document Type: " + result.get("document_type"));
        System.out.println("Confidentiality Level: " + result.get("confidentiality_level"));
        System.out.println("Detected Data Types: " + detectedDataTypes);
        System.out.println("Risks Count: " + (risks != null ? risks.size() : 0));
        System.out.println("Risks Details: " + risksDetails);
        System.out.println("Compliance Standards: " + complianceStandards);
        System.out.println("================================");

        List<AnalysisResult> existingResults = analysisResultRepository.findByDocumentIdOrderByCreatedAtDesc(document.getId());
        AnalysisResult analysisResult = existingResults.isEmpty() ? new AnalysisResult() : existingResults.get(0);
        if (existingResults.size() > 1) {
            analysisResultRepository.deleteAll(existingResults.subList(1, existingResults.size()));
        }
        analysisResult.setDocument(document);
        analysisResult.setDocumentType(document.getDocumentType() != null ? document.getDocumentType().name() : null);
        analysisResult.setConfidentialityLevel(document.getConfidentialityLevel() != null ? document.getConfidentialityLevel().name() : null);
        analysisResult.setPiiDetected(sensitiveItems != null && !sensitiveItems.isEmpty());
        analysisResult.setRisksCount(risks != null ? risks.size() : 0);
        analysisResult.setComplianceStandards(complianceStandards);
        analysisResult.setSecurityScore(securityScore);
        analysisResult.setDetectedDataTypes(detectedDataTypes);
        analysisResult.setRisksDetails(risksDetails);
        analysisResultRepository.save(analysisResult);
    }

    private Integer calculateSecurityScore(Map<String, Object> result,
                                            List<Map<String, Object>> sensitiveItems,
                                            List<Map<String, Object>> risks) {
        int baseScore = 100;
        int sensitiveDataDeduction = sensitiveItems != null ? sensitiveItems.size() * 5 : 0;

        int risksDeduction = 0;
        if (risks != null) {
            for (Map<String, Object> risk : risks) {
                String severity = getString(risk, "severity", null).toUpperCase(Locale.ROOT);
                if ("CRITIQUE".equals(severity)) {
                    risksDeduction += 20;
                } else if ("ELEVE".equals(severity) || "ELEVÉ".equals(severity) || "ÉLEVÉ".equals(severity)) {
                    risksDeduction += 15;
                } else if ("MOYEN".equals(severity)) {
                    risksDeduction += 10;
                } else if ("FAIBLE".equals(severity)) {
                    risksDeduction += 5;
                }
            }
        }

        int confidentialityAdjustment = 0;
        Object confLevel = result.get("confidentiality_level");
        if (confLevel == null) confLevel = result.get("confidentialityLevel");
        String confidentiality = confLevel != null ? confLevel.toString().toUpperCase(Locale.ROOT) : null;
        if ("PUBLIC".equals(confidentiality)) {
            confidentialityAdjustment = 5;
        } else if ("INTERNE".equals(confidentiality)) {
            confidentialityAdjustment = 0;
        } else if ("CONFIDENTIEL".equals(confidentiality)) {
            confidentialityAdjustment = -5;
        } else if ("TRES_CONFIDENTIEL".equals(confidentiality) || "TRÈS_CONFIDENTIEL".equals(confidentiality)) {
            confidentialityAdjustment = -10;
        }

        int typeAdjustment = 0;
        Object docType = result.get("document_type");
        if (docType == null) docType = result.get("documentType");
        String documentType = docType != null ? docType.toString().toUpperCase(Locale.ROOT).replace(" ", "_") : null;
        if ("DOSSIER_RH".equals(documentType) || "RH".equals(documentType)) {
            typeAdjustment = -5;
        } else if ("CONTRAT".equals(documentType)) {
            typeAdjustment = -3;
        } else if ("FACTURE".equals(documentType)) {
            typeAdjustment = -4;
        } else if ("POLITIQUE_SSI".equals(documentType) || "POLITIQUE_SECURITE".equals(documentType)) {
            typeAdjustment = 5;
        }

        int securityScore = baseScore - sensitiveDataDeduction - risksDeduction + confidentialityAdjustment + typeAdjustment;
        return Math.max(0, Math.min(100, securityScore));
    }

    private void saveSensitiveData(Document document, Map<String, Object> item) {
        try {
            if (document == null || document.getId() == null) {
                System.out.println("Skipping sensitive data save: document is null or has no id");
                return;
            }

            Object typeObj = item.get("data_type");
            if (typeObj == null) {
                typeObj = item.get("dataType");
            }
            if (typeObj == null) {
                return;
            }
            String typeStr = typeObj.toString();

            SensitiveDataType dataType;
            try {
                dataType = SensitiveDataType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                dataType = SensitiveDataType.AUTRE;
            }

            Object confidence = item.get("confidence");
            SensitiveData sensitiveData = SensitiveData.builder()
                    .document(document)
                    .dataType(dataType)
                    .detectedValue(getString(item, "detected_value", "detectedValue"))
                    .maskedValue(getString(item, "masked_value", "maskedValue"))
                    .confidence(confidence != null ? new BigDecimal(confidence.toString()) : BigDecimal.ZERO)
                    .build();
            sensitiveDataRepository.save(sensitiveData);
        } catch (Exception e) {
            System.out.println("Error saving sensitive data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Risk saveRisk(Document document, Map<String, Object> riskData) {
        String rawTitle = normalizeText(getString(riskData, "title", null));
        String title = rawTitle.isBlank() ? "Risque détecté par l'analyse IA" : rawTitle;

        String description = normalizeText(getString(riskData, "description", null));
        String severityStr = riskData.get("severity") != null ? riskData.get("severity").toString() : "MOYEN";
        RiskSeverity severity;
        try {
            severity = RiskSeverity.valueOf(severityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            severity = RiskSeverity.MOYEN;
        }

        List<Risk> existingRisks = riskRepository.findByDocumentId(document.getId());
        Optional<Risk> existingRisk = existingRisks.stream()
                .filter(r -> title.equalsIgnoreCase(normalizeText(r.getTitle())))
                .findFirst();

        if (existingRisk.isPresent()) {
            Risk risk = existingRisk.get();
            risk.setDescription(description);
            risk.setSeverity(severity);
            risk.setCategory(normalizeText(getString(riskData, "category", null)));
            risk.setStatus(RiskStatus.OUVERT);
            return riskRepository.save(risk);
        }

        return riskRepository.save(Risk.builder()
                .document(document)
                .title(title)
                .description(description)
                .severity(severity)
                .category(normalizeText(getString(riskData, "category", null)))
                .status(RiskStatus.OUVERT)
                .build());
    }

    private void saveRecommendation(Risk risk, Map<String, Object> recData) {
        String description = normalizeText(getString(recData, "description", null));
        if (description.isBlank()) {
            description = normalizeText(getString(recData, "recommendation", null));
        }
        if (description.isBlank()) {
            return;
        }
        final String normalizedDescription = description;
        String priorityStr = recData.get("priority") != null ? recData.get("priority").toString() : "MOYENNE";
        RecommendationPriority priority;
        try {
            priority = RecommendationPriority.valueOf(priorityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            priority = RecommendationPriority.MOYENNE;
        }

        List<Recommendation> existingRecommendations = recommendationRepository.findByRiskId(risk.getId());
        Optional<Recommendation> existing = existingRecommendations.stream()
                .filter(rec -> normalizedDescription.equalsIgnoreCase(normalizeText(rec.getDescription())))
                .findFirst();

        if (existing.isPresent()) {
            Recommendation recommendation = existing.get();
            recommendation.setPriority(priority);
            recommendation.setStatus(RecommendationStatus.PROPOSEE);
            recommendationRepository.save(recommendation);
            return;
        }

        recommendationRepository.save(Recommendation.builder()
                .risk(risk)
                .description(description)
                .priority(priority)
                .status(RecommendationStatus.PROPOSEE)
                .build());
    }

    private boolean containsRecommendationContent(Map<String, Object> recommendationData) {
        if (recommendationData == null) {
            return false;
        }
        String description = normalizeText(getString(recommendationData, "description", null));
        if (!description.isBlank()) {
            return true;
        }
        String recommendation = normalizeText(getString(recommendationData, "recommendation", null));
        return !recommendation.isBlank();
    }

    private DocumentType mapDocumentType(String value) {
        if (value == null || value.isBlank()) {
            return DocumentType.AUTRE;
        }
        String normalized = value.toUpperCase().replace(" ", "_").replace("-", "_");
        if ("POLITIQUE_SECURITE".equals(normalized)) {
            normalized = "POLITIQUE_SSI";
        }
        if ("RH".equals(normalized) || "DOSSIER_RH".equals(normalized)) {
            return DocumentType.DOSSIER_RH;
        }
        if ("FINANCIER".equals(normalized) || "FACTURE".equals(normalized)) {
            return DocumentType.FACTURE;
        }
        if ("TECHNIQUE".equals(normalized) || "JURIDIQUE".equals(normalized) || "COMPTABLE".equals(normalized) || "COMMERCIAL".equals(normalized)) {
            return DocumentType.PROCEDURE;
        }
        if ("RAPPORT".equals(normalized)) {
            return DocumentType.RAPPORT;
        }
        try {
            return DocumentType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return DocumentType.AUTRE;
        }
    }

    private ConfidentialityLevel mapConfidentiality(String value) {
        if (value == null || value.isBlank()) {
            return ConfidentialityLevel.INTERNE;
        }
        String normalized = value.toUpperCase().trim();
        if ("NON_CLASSIFIE".equals(normalized) || "NONE".equals(normalized)) {
            return ConfidentialityLevel.INTERNE;
        }
        if ("TRES_CONFIDENTIEL".equals(normalized) || "TRÈS_CONFIDENTIEL".equals(normalized)) {
            return ConfidentialityLevel.TRES_CONFIDENTIEL;
        }
        if ("SECRET".equals(normalized) || "SECRETS".equals(normalized)) {
            return ConfidentialityLevel.TRES_CONFIDENTIEL;
        }
        try {
            return ConfidentialityLevel.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return ConfidentialityLevel.INTERNE;
        }
    }

    private String getString(Map<String, Object> map, String snakeKey, String camelKey) {
        Object val = map.get(snakeKey);
        if (val == null && camelKey != null) {
            val = map.get(camelKey);
        }
        return val != null ? val.toString() : "";
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private RiskResponse toRiskResponse(Risk risk) {
        return RiskResponse.builder()
                .id(risk.getId())
                .documentId(risk.getDocument() != null ? risk.getDocument().getId() : null)
                .title(risk.getTitle())
                .description(risk.getDescription())
                .severity(risk.getSeverity())
                .category(risk.getCategory())
                .status(risk.getStatus())
                .detectedAt(risk.getDetectedAt())
                .build();
    }

    private Map<String, Object> performFallbackAnalysis(Document document) {
        Map<String, Object> result = new HashMap<>();
        
        // Use existing document type and confidentiality level
        result.put("document_type", document.getDocumentType() != null ? document.getDocumentType().name() : "AUTRE");
        result.put("confidentiality_level", document.getConfidentialityLevel() != null ? document.getConfidentialityLevel().name() : "INTERNE");
        result.put("extracted_text", "Analyse locale effectuée (service IA indisponible)");
        
        System.out.println("=== FALLBACK ANALYSIS START ===");
        System.out.println("Document: " + document.getFileName());
        System.out.println("Document Type: " + document.getDocumentType());
        System.out.println("Confidentiality Level: " + document.getConfidentialityLevel());
        
        // Generate sensitive data based on document type
        List<Map<String, Object>> sensitiveData = new ArrayList<>();
        String fileName = document.getFileName().toLowerCase();
        
        if (document.getDocumentType() == DocumentType.DOSSIER_RH || fileName.contains("cv") || fileName.contains("employe")) {
            sensitiveData.add(createSensitiveData("CIN", "AB123456", "AB******", 0.92));
            sensitiveData.add(createSensitiveData("TELEPHONE", "0612345678", "06******78", 0.88));
            sensitiveData.add(createSensitiveData("EMAIL", "email@example.com", "e***@example.com", 0.85));
        } else if (document.getDocumentType() == DocumentType.CONTRAT || fileName.contains("contrat")) {
            sensitiveData.add(createSensitiveData("IBAN", "FR7612345678901234567890123", "FR76*******************23", 0.95));
            sensitiveData.add(createSensitiveData("AUTRE", "Signature électronique", "********", 0.90));
        } else if (document.getDocumentType() == DocumentType.FACTURE || fileName.contains("facture")) {
            sensitiveData.add(createSensitiveData("BANQUE", "1250.00 €", "****.** €", 0.87));
            sensitiveData.add(createSensitiveData("IBAN", "FR7612345678901234567890123", "FR76*******************23", 0.93));
        } else if (fileName.contains("budget") || fileName.contains("comptable")) {
            sensitiveData.add(createSensitiveData("BANQUE", "50000.00 €", "*****.** €", 0.89));
        }
        
        System.out.println("Generated sensitive data count: " + sensitiveData.size());
        result.put("sensitive_data", sensitiveData);
        
        // Generate risks based on document type and confidentiality
        List<Map<String, Object>> risks = new ArrayList<>();
        
        if (document.getConfidentialityLevel() == ConfidentialityLevel.TRES_CONFIDENTIEL) {
            risks.add(createRisk("Fuite de données critiques", "Document contenant des informations très sensibles pouvant causer des dommages majeurs en cas de fuite", "CRITIQUE", "Confidentialité"));
            risks.add(createRisk("Accès non autorisé", "Risque d'accès par des utilisateurs non autorisés", "ELEVE", "Contrôle d'accès"));
        } else if (document.getConfidentialityLevel() == ConfidentialityLevel.CONFIDENTIEL) {
            risks.add(createRisk("Données sensibles exposées", "Document contenant des informations sensibles nécessitant une protection renforcée", "ELEVE", "Confidentialité"));
        }
        
        if (document.getDocumentType() == DocumentType.DOSSIER_RH || fileName.contains("cv")) {
            risks.add(createRisk("Violation de la vie privée", "Données personnelles d'employés nécessitant une protection RGPD", "ELEVE", "RGPD"));
        } else if (document.getDocumentType() == DocumentType.CONTRAT || fileName.contains("contrat")) {
            risks.add(createRisk("Non-respect contractuelle", "Risque de divulgation d'informations contractuelles confidentielles", "MOYEN", "Juridique"));
        } else if (document.getDocumentType() == DocumentType.FACTURE || fileName.contains("facture")) {
            risks.add(createRisk("Fraude financière", "Informations financières sensibles pouvant être utilisées pour des fraudes", "MOYEN", "Financière"));
        } else if (document.getDocumentType() == DocumentType.POLITIQUE_SSI || fileName.contains("politique") || fileName.contains("procedure")) {
            risks.add(createRisk("Obsolescence des procédures", "Document de sécurité pouvant ne plus être à jour", "FAIBLE", "Gouvernance"));
        }
        
        // If no risks generated, add a generic one
        if (risks.isEmpty()) {
            risks.add(createRisk("Classification incomplète", "Document nécessitant une révision de sa classification", "FAIBLE", "Classification"));
        }
        
        System.out.println("Generated risks count: " + risks.size());
        result.put("risks", risks);
        
        // Calculate security score based on actual data (sensitive data count, risks, confidentiality)
        int baseScore = 100;
        
        // Deduct for each sensitive data item
        int sensitiveDataDeduction = sensitiveData.size() * 5;
        
        // Deduct for risks based on severity
        int risksDeduction = 0;
        for (Map<String, Object> risk : risks) {
            String severity = (String) risk.get("severity");
            if ("CRITIQUE".equals(severity)) risksDeduction += 20;
            else if ("ELEVE".equals(severity)) risksDeduction += 15;
            else if ("MOYEN".equals(severity)) risksDeduction += 10;
            else if ("FAIBLE".equals(severity)) risksDeduction += 5;
        }
        
        // Adjust based on confidentiality level
        int confidentialityAdjustment = 0;
        if (document.getConfidentialityLevel() == ConfidentialityLevel.PUBLIC) confidentialityAdjustment = 5;
        else if (document.getConfidentialityLevel() == ConfidentialityLevel.INTERNE) confidentialityAdjustment = 0;
        else if (document.getConfidentialityLevel() == ConfidentialityLevel.CONFIDENTIEL) confidentialityAdjustment = -5;
        else if (document.getConfidentialityLevel() == ConfidentialityLevel.TRES_CONFIDENTIEL) confidentialityAdjustment = -10;
        
        // Adjust based on document type
        int typeAdjustment = 0;
        if (document.getDocumentType() == DocumentType.DOSSIER_RH) typeAdjustment = -5;
        else if (document.getDocumentType() == DocumentType.CONTRAT) typeAdjustment = -3;
        else if (document.getDocumentType() == DocumentType.FACTURE) typeAdjustment = -4;
        else if (document.getDocumentType() == DocumentType.POLITIQUE_SSI) typeAdjustment = 5;
        
        // Calculate final score (deterministic based on actual data)
        int securityScore = baseScore - sensitiveDataDeduction - risksDeduction + confidentialityAdjustment + typeAdjustment;
        securityScore = Math.max(0, Math.min(100, securityScore));
        result.put("security_score", securityScore);
        
        System.out.println("=== FALLBACK ANALYSIS SCORE CALCULATION ===");
        System.out.println("Document ID: " + document.getId());
        System.out.println("Document Type: " + document.getDocumentType());
        System.out.println("Confidentiality Level: " + document.getConfidentialityLevel());
        System.out.println("Sensitive Data Count: " + sensitiveData.size());
        System.out.println("Sensitive Data Deduction: " + sensitiveDataDeduction);
        System.out.println("Risks Count: " + risks.size());
        System.out.println("Risks Deduction: " + risksDeduction);
        System.out.println("Confidentiality Adjustment: " + confidentialityAdjustment);
        System.out.println("Type Adjustment: " + typeAdjustment);
        System.out.println("FINAL SECURITY SCORE: " + securityScore);
        System.out.println("==========================================");
        
        // Generate recommendations based on risks
        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (Map<String, Object> risk : risks) {
            String severity = (String) risk.get("severity");
            String category = (String) risk.get("category");
            
            if ("CRITIQUE".equals(severity)) {
                recommendations.add(createRecommendation("Chiffrement immédiat du document", "HAUTE"));
                recommendations.add(createRecommendation("Restriction stricte des accès", "HAUTE"));
            } else if ("ELEVE".equals(severity)) {
                recommendations.add(createRecommendation("Renforcer les contrôles d'accès", "MOYENNE"));
                if ("RGPD".equals(category)) {
                    recommendations.add(createRecommendation("Conformité RGPD à vérifier", "HAUTE"));
                }
            } else if ("MOYEN".equals(severity)) {
                recommendations.add(createRecommendation("Surveillance régulière recommandée", "MOYENNE"));
            } else {
                recommendations.add(createRecommendation("Révision périodique conseillée", "BASSE"));
            }
        }
        result.put("recommendations", recommendations);
        
        // Compliance standards based on document type
        List<String> standards = new ArrayList<>();
        standards.add("ISO27001");
        if (document.getDocumentType() == DocumentType.DOSSIER_RH || fileName.contains("cv") || fileName.contains("employe")) {
            standards.add("RGPD");
        }
        if (document.getDocumentType() == DocumentType.CONTRAT || fileName.contains("contrat")) {
            standards.add("RGPD");
            standards.add("CCPA");
        }
        result.put("compliance_standards", String.join(", ", standards));
        
        return result;
    }
    
    private Map<String, Object> createSensitiveData(String dataType, String detectedValue, String maskedValue, double confidence) {
        Map<String, Object> data = new HashMap<>();
        data.put("data_type", dataType);
        data.put("detected_value", detectedValue);
        data.put("masked_value", maskedValue);
        data.put("confidence", confidence);
        return data;
    }
    
    private Map<String, Object> createRisk(String title, String description, String severity, String category) {
        Map<String, Object> risk = new HashMap<>();
        risk.put("title", title);
        risk.put("description", description);
        risk.put("severity", severity);
        risk.put("category", category);
        return risk;
    }
    
    private Map<String, Object> createRecommendation(String recommendation, String priority) {
        Map<String, Object> rec = new HashMap<>();
        rec.put("recommendation", recommendation);
        rec.put("priority", priority);
        return rec;
    }
}
