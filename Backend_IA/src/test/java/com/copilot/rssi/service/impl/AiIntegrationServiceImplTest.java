package com.copilot.rssi.service.impl;

import com.copilot.rssi.entity.AnalysisResult;
import com.copilot.rssi.entity.Document;
import com.copilot.rssi.entity.Recommendation;
import com.copilot.rssi.entity.Risk;
import com.copilot.rssi.entity.SensitiveData;
import com.copilot.rssi.entity.enums.ConfidentialityLevel;
import com.copilot.rssi.entity.enums.DocumentType;
import com.copilot.rssi.repository.AnalysisResultRepository;
import com.copilot.rssi.repository.DocumentRepository;
import com.copilot.rssi.repository.RecommendationRepository;
import com.copilot.rssi.repository.RiskRepository;
import com.copilot.rssi.repository.SensitiveDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiIntegrationServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private SensitiveDataRepository sensitiveDataRepository;

    @Mock
    private RiskRepository riskRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private AnalysisResultRepository analysisResultRepository;

    @InjectMocks
    private AiIntegrationServiceImpl service;

    @Test
    void applyAnalysisResultDoesNotCreateSyntheticRecommendationWhenAiPayloadHasNoRecommendations() throws Exception {
        Document document = new Document();
        document.setId(1L);
        document.setDocumentType(DocumentType.AUTRE);
        document.setConfidentialityLevel(ConfidentialityLevel.INTERNE);

        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(analysisResultRepository.findByDocumentIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(analysisResultRepository.save(any(AnalysisResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(riskRepository.findByDocumentId(1L)).thenReturn(Collections.emptyList());
        when(riskRepository.save(any(Risk.class))).thenAnswer(invocation -> {
            Risk risk = invocation.getArgument(0);
            risk.setId(100L);
            return risk;
        });

        Map<String, Object> aiResult = new HashMap<>();
        aiResult.put("risks", List.of(Map.of(
                "title", "Risque détecté",
                "description", "Description du risque",
                "severity", "MOYEN",
                "category", "Confidentialité"
        )));

        Method applyAnalysisResult = AiIntegrationServiceImpl.class.getDeclaredMethod("applyAnalysisResult", Document.class, Map.class);
        applyAnalysisResult.setAccessible(true);
        applyAnalysisResult.invoke(service, document, aiResult);

        verify(recommendationRepository, never()).save(any(Recommendation.class));
    }
}
