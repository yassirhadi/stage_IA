-- Check current data status in database
-- Sélectionnez votre base MySQL avant d’exécuter ce script.

-- Check documents count
SELECT 'documents' as table_name, COUNT(*) as count FROM documents;

-- Check sensitive_data count
SELECT 'sensitive_data' as table_name, COUNT(*) as count FROM sensitive_data;

-- Check risks count
SELECT 'risks' as table_name, COUNT(*) as count FROM risks;

-- Check reports count
SELECT 'reports' as table_name, COUNT(*) as count FROM reports;

-- Show sample documents
SELECT id, file_name, created_at FROM documents LIMIT 5;

-- Show sample sensitive_data
SELECT id, data_type, detected_value, created_at FROM sensitive_data LIMIT 5;

-- Show sample risks
SELECT id, title, severity, status FROM risks LIMIT 5;

-- Show sample reports
SELECT id, title, report_type, overall_score, generated_at FROM reports LIMIT 5;
