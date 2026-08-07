package com.copilot.rssi.service.impl;

import com.copilot.rssi.dto.request.UpdateRecommendationRequest;
import com.copilot.rssi.dto.response.RecommendationResponse;
import com.copilot.rssi.entity.Recommendation;
import com.copilot.rssi.exception.ResourceNotFoundException;
import com.copilot.rssi.mapper.EntityMapper;
import com.copilot.rssi.repository.RecommendationRepository;
import com.copilot.rssi.repository.RiskRepository;
import com.copilot.rssi.service.interfaces.AuditService;
import com.copilot.rssi.service.interfaces.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RiskRepository riskRepository;
    private final EntityMapper mapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public List<RecommendationResponse> getAll() {
        // Older analysis runs left rows pointing at deleted risks. Remove those
        // rows before loading the EAGER risk association, otherwise one orphan
        // causes the complete endpoint to fail with HTTP 500.
        recommendationRepository.deleteOrphanedRecommendations();

        // Every detected risk must have an actionable recommendation. This also
        // repairs risks created by analyses whose AI response omitted the list.
        for (var risk : riskRepository.findAll()) {
            if (recommendationRepository.findByRiskId(risk.getId()).isEmpty()) {
                recommendationRepository.save(Recommendation.builder()
                        .risk(risk)
                        .description("Analyser le risque « " + risk.getTitle() + " » et appliquer une mesure corrective.")
                        .status(com.copilot.rssi.entity.enums.RecommendationStatus.PROPOSEE)
                        .priority(com.copilot.rssi.entity.enums.RecommendationPriority.MOYENNE)
                        .build());
            }
        }

        return recommendationRepository.findAllWithRisk().stream()
                .map(mapper::toRecommendationResponse)
                .toList();
    }

    @Override
    @Transactional
    public RecommendationResponse updateStatus(Long id, UpdateRecommendationRequest request, String username) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recommandation non trouvée"));
        rec.setStatus(request.getStatus());
        Recommendation saved = recommendationRepository.save(rec);
        auditService.log(username, "UPDATE_RECOMMENDATION", "Recommendation", id,
                "Statut changé en " + request.getStatus());
        return mapper.toRecommendationResponse(saved);
    }
}
