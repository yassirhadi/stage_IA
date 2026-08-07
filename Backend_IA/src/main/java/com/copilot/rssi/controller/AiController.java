package com.copilot.rssi.controller;

import com.copilot.rssi.dto.request.ChatRequest;
import com.copilot.rssi.dto.response.ApiResponse;
import com.copilot.rssi.dto.response.RiskResponse;
import com.copilot.rssi.entity.Risk;
import com.copilot.rssi.repository.RiskRepository;
import com.copilot.rssi.service.interfaces.AiIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "Intelligence Artificielle")
public class AiController {

    private final AiIntegrationService aiIntegrationService;
    private final RiskRepository riskRepository;

    @PostMapping("/chat")
    @Operation(summary = "Poser une question au Copilote IA")
    public ApiResponse<Map<String, Object>> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(aiIntegrationService.chat(request));
    }

    @GetMapping("/risks")
    @Operation(summary = "Lister tous les risques détectés")
    public ApiResponse<List<Map<String, Object>>> getRisks() {
        List<Risk> risks = riskRepository.findAll();
        List<Map<String, Object>> result = risks.stream()
            .map(r -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", r.getId());
                map.put("title", r.getTitle());
                map.put("description", r.getDescription());
                map.put("severity", r.getSeverity() != null ? r.getSeverity().name() : null);
                map.put("category", r.getCategory());
                map.put("status", r.getStatus() != null ? r.getStatus().name() : null);
                map.put("documentId", r.getDocument() != null ? r.getDocument().getId() : null);
                map.put("detectedAt", r.getDetectedAt() != null ? r.getDetectedAt().toString() : null);
                return map;
            })
            .collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    @GetMapping("/dashboard-stats")
    @Operation(summary = "Statistiques du dashboard")
    public ApiResponse<Map<String, Object>> getDashboardStats() {
        List<Risk> risks = riskRepository.findAll();
        long criticalRisks = risks.stream().filter(r -> r.getSeverity() != null && r.getSeverity().name().equals("CRITIQUE")).count();
        long highRisks = risks.stream().filter(r -> r.getSeverity() != null && r.getSeverity().name().equals("ELEVE")).count();
        long mediumRisks = risks.stream().filter(r -> r.getSeverity() != null && r.getSeverity().name().equals("MOYEN")).count();
        long lowRisks = risks.stream().filter(r -> r.getSeverity() != null && r.getSeverity().name().equals("FAIBLE")).count();

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalRisks", risks.size());
        stats.put("criticalRisks", criticalRisks);
        stats.put("highRisks", highRisks);
        stats.put("mediumRisks", mediumRisks);
        stats.put("lowRisks", lowRisks);
        return ApiResponse.ok(stats);
    }

    @PutMapping("/risks/{id}/status")
    @Operation(summary = "Mettre à jour le statut d'un risque")
    public ApiResponse<Map<String, Object>> updateRiskStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        return riskRepository.findById(id).map(risk -> {
            String status = request.get("status");
            if (status != null) {
                try {
                    risk.setStatus(com.copilot.rssi.entity.enums.RiskStatus.valueOf(status));
                    riskRepository.save(risk);
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("id", risk.getId());
                    result.put("status", risk.getStatus().name());
                    return ApiResponse.ok(result);
                } catch (IllegalArgumentException e) {
                    Map<String, Object> error = new java.util.HashMap<>();
                    error.put("error", "Invalid status value");
                    return ApiResponse.error("Invalid status value", error);
                }
            }
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", "Status is required");
            return ApiResponse.error("Status is required", error);
        }).orElse(ApiResponse.error("Risk not found", null));
    }

    @DeleteMapping("/risks/{id}")
    @Operation(summary = "Supprimer un risque")
    public ApiResponse<Void> deleteRisk(@PathVariable Long id) {
        if (riskRepository.existsById(id)) {
            riskRepository.deleteById(id);
            return ApiResponse.ok("Risk deleted", null);
        }
        return ApiResponse.error("Risk not found", null);
    }
}
