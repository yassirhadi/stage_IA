-- Add UNIQUE constraints to prevent future duplicates
-- Run this ONCE only after database schema is created

-- Sélectionnez votre base MySQL avant d’exécuter ce script.

-- Add UNIQUE constraint on documents.file_path (better than file_name for same files in different folders)
-- Drop constraint if exists first to avoid errors on re-run
ALTER TABLE documents
DROP INDEX IF EXISTS uk_document_file_path;

ALTER TABLE documents
ADD CONSTRAINT uk_document_file_path UNIQUE (file_path);

-- Add UNIQUE constraint on sensitive_data (document_id, data_type, detected_value)
ALTER TABLE sensitive_data
DROP INDEX IF EXISTS uk_sensitive_data;

ALTER TABLE sensitive_data
ADD CONSTRAINT uk_sensitive_data UNIQUE (document_id, data_type, detected_value);

-- Add UNIQUE constraint on risks (document_id, title)
ALTER TABLE risks
DROP INDEX IF EXISTS uk_risk;

ALTER TABLE risks
ADD CONSTRAINT uk_risk UNIQUE (document_id, title);

-- Note: No UNIQUE constraint on reports (title, report_type) because multiple reports of same type are normal

SELECT 'UNIQUE constraints added successfully' as status;
