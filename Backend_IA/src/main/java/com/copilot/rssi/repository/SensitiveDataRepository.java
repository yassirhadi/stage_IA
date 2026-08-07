package com.copilot.rssi.repository;

import com.copilot.rssi.entity.SensitiveData;
import com.copilot.rssi.entity.enums.SensitiveDataType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SensitiveDataRepository extends JpaRepository<SensitiveData, Long> {
    List<SensitiveData> findByDocumentId(Long documentId);
    long countByDocumentId(Long documentId);
    List<SensitiveData> findByDataType(SensitiveDataType dataType);
    void deleteByDocumentId(Long documentId);

    @Query("SELECT DISTINCT sd.document FROM SensitiveData sd WHERE sd.dataType = :dataType")
    List<com.copilot.rssi.entity.Document> findDistinctDocumentsByDataType(SensitiveDataType dataType);

    @Query("SELECT DISTINCT sd.document FROM SensitiveData sd")
    List<com.copilot.rssi.entity.Document> findDistinctDocumentsWithSensitiveData();

    @Query(value = "SELECT sd.id, sd.document_id, d.file_name, sd.data_type, sd.detected_value, sd.masked_value, sd.confidence, sd.created_at FROM sensitive_data sd LEFT JOIN documents d ON sd.document_id = d.id", nativeQuery = true)
    List<Object[]> findAllNative();
}
