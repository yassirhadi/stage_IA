-- Clean database (truncate tables)
-- Run this to clear data before re-inserting

-- Sélectionnez votre base MySQL avant d’exécuter ce script.

-- Disable foreign key checks temporarily for truncation
SET FOREIGN_KEY_CHECKS = 0;

-- Truncate tables in correct order (child tables first)
TRUNCATE TABLE sensitive_data;
TRUNCATE TABLE risks;
TRUNCATE TABLE documents;
-- Note: We keep reports table intact as it contains system-generated reports

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Verify cleanup results
SELECT 'Cleanup completed - tables truncated' as status;
SELECT COUNT(*) as documents_count FROM documents;
SELECT COUNT(*) as sensitive_data_count FROM sensitive_data;
SELECT COUNT(*) as risks_count FROM risks;
SELECT COUNT(*) as reports_count FROM reports;
