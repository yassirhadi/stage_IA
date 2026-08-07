package com.copilot.rssi.repository;

import com.copilot.rssi.entity.Recommendation;
import com.copilot.rssi.entity.enums.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByRiskId(Long riskId);

    @Query("SELECT COUNT(r) FROM Recommendation r WHERE r.risk.document.id = :documentId")
    long countByDocumentId(@Param("documentId") Long documentId);
    List<Recommendation> findByStatus(RecommendationStatus status);

    @Query("select r from Recommendation r join fetch r.risk")
    List<Recommendation> findAllWithRisk();

    @Modifying
    @Transactional
    @Query(value = "delete from recommendations where risk_id not in (select id from risks)", nativeQuery = true)
    int deleteOrphanedRecommendations();
}
