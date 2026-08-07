package com.copilot.rssi.repository;

import com.copilot.rssi.entity.Document;
import com.copilot.rssi.entity.enums.AnalysisStatus;
import com.copilot.rssi.entity.enums.ConfidentialityLevel;
import com.copilot.rssi.entity.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByFilePath(String filePath);
    Optional<Document> findFirstByFilePathOrderByIdDesc(String filePath);
    List<Document> findByDocumentType(DocumentType documentType);
    List<Document> findByConfidentialityLevel(ConfidentialityLevel level);
    List<Document> findByAnalysisStatus(AnalysisStatus status);
    long countByDocumentType(DocumentType documentType);

    @Query(value = "SELECT id, file_name, file_path, extension, file_size, mime_type, created_date, modified_date, document_type, confidentiality_level, analysis_status, created_at FROM documents", nativeQuery = true)
    List<Object[]> findAllNative();
}
