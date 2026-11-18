-- Manual data insertion for RSSI tables
-- Run this file manually if data.sql was not executed by Spring Boot

-- Sélectionnez votre base MySQL avant d’exécuter ce script.

-- 1. Insert documents (required for sensitive_data foreign key)
INSERT IGNORE INTO documents (file_name, file_path, file_size, extension, document_type, confidentiality_level, analysis_status, created_at, updated_at) VALUES
('fiche_employe.txt', '/storage/documents/fiche_employe.txt', 2048, 'txt', 'DOSSIER_RH', 'CONFIDENTIEL', 'COMPLETED', NOW(), NOW()),
('contrat_travail.txt', '/storage/documents/contrat_travail.txt', 4096, 'txt', 'CONTRAT', 'CONFIDENTIEL', 'COMPLETED', NOW(), NOW()),
('CV_Ahmed_Benali.txt', '/storage/documents/CV_Ahmed_Benali.txt', 1024, 'txt', 'DOSSIER_RH', 'INTERNE', 'COMPLETED', NOW(), NOW()),
('facture_2025.txt', '/storage/documents/facture_2025.txt', 512, 'txt', 'FACTURE', 'TRES_CONFIDENTIEL', 'COMPLETED', NOW(), NOW()),
('budget_2025.txt', '/storage/documents/budget_2025.txt', 3072, 'txt', 'FACTURE', 'TRES_CONFIDENTIEL', 'COMPLETED', NOW(), NOW()),
('politique_ssi.txt', '/storage/documents/politique_ssi.txt', 8192, 'txt', 'POLITIQUE_SSI', 'INTERNE', 'COMPLETED', NOW(), NOW());

-- 2. Insert sensitive_data using subqueries
INSERT IGNORE INTO sensitive_data (document_id, data_type, detected_value, masked_value, confidence)
SELECT 
    (SELECT id FROM documents WHERE file_name = 'fiche_employe.txt' LIMIT 1), 'CIN', 'AB123456', 'AB******', 0.95
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'fiche_employe.txt' LIMIT 1), 'TELEPHONE', '0612345678', '06******78', 0.90
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'fiche_employe.txt' LIMIT 1), 'ADRESSE', '123 Rue Hassan, Casablanca', '123 Rue Hassan, Casablanca', 0.85
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'contrat_travail.txt' LIMIT 1), 'CIN', 'CD789012', 'CD******', 0.95
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'contrat_travail.txt' LIMIT 1), 'TELEPHONE', '0623456789', '06******89', 0.90
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'CV_Ahmed_Benali.txt' LIMIT 1), 'CIN', 'EF345678', 'EF******', 0.95
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'CV_Ahmed_Benali.txt' LIMIT 1), 'EMAIL', 'ahmed.benali@email.com', 'a********@email.com', 0.92
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'CV_Ahmed_Benali.txt' LIMIT 1), 'TELEPHONE', '0634567890', '06******90', 0.90
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'facture_2025.txt' LIMIT 1), 'IBAN', 'MA760123456789012345678901', 'MA76************************', 0.98
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'facture_2025.txt' LIMIT 1), 'RIB', '123456789012345678901234', '12********************', 0.95
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'budget_2025.txt' LIMIT 1), 'IBAN', 'MA760987654321098765432109', 'MA76************************', 0.98
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'budget_2025.txt' LIMIT 1), 'BANQUE', 'Attijariwafa Bank', 'Attijariwafa Bank', 0.88
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'budget_2025.txt' LIMIT 1), 'FISCAL', '12345678', '12345678', 0.85
UNION ALL
SELECT 
    (SELECT id FROM documents WHERE file_name = 'politique_ssi.txt' LIMIT 1), 'IBAN', 'MA760112233445566778899001', 'MA76************************', 0.98;

-- 3. Insert risks using subqueries (only fields that exist in Risk entity)
INSERT IGNORE INTO risks (title, description, severity, category, status, document_id, detected_at)
SELECT 
    'Données bancaires exposées en clair', 
    'Le document facture_2025.txt contient un numéro IBAN non masqué, ce qui constitue une violation de la sécurité.', 
    'CRITIQUE', 
    'Données sensibles', 
    'OUVERT', 
    (SELECT id FROM documents WHERE file_name = 'facture_2025.txt' LIMIT 1), 
    NOW()
UNION ALL
SELECT 
    'CIN non protégé dans les documents RH', 
    'Les documents fiche_employe.txt et contrat_travail.txt contiennent des numéros CIN en clair.', 
    'ELEVE', 
    'Données personnelles', 
    'OUVERT', 
    (SELECT id FROM documents WHERE file_name = 'fiche_employe.txt' LIMIT 1), 
    NOW()
UNION ALL
SELECT 
    'Emails professionnels exposés', 
    'Le CV contient une adresse email professionnelle visible.', 
    'MOYEN', 
    'Données personnelles', 
    'OUVERT', 
    (SELECT id FROM documents WHERE file_name = 'CV_Ahmed_Benali.txt' LIMIT 1), 
    NOW()
UNION ALL
SELECT 
    'Coordonnées bancaires non sécurisées', 
    'Le document budget_2025.txt contient des informations bancaires sensibles.', 
    'ELEVE', 
    'Données financières', 
    'OUVERT', 
    (SELECT id FROM documents WHERE file_name = 'budget_2025.txt' LIMIT 1), 
    NOW()
UNION ALL
SELECT 
    'Numéros de téléphone visibles', 
    'Plusieurs documents contiennent des numéros de téléphone en clair.', 
    'FAIBLE', 
    'Données de contact', 
    'OUVERT', 
    (SELECT id FROM documents WHERE file_name = 'fiche_employe.txt' LIMIT 1), 
    NOW();

-- 4. Insert reports (only fields that exist in Report entity)
INSERT IGNORE INTO reports (title, report_type, executive_summary, overall_score, rssi_signature, generated_at, content) VALUES
('Rapport de conformité RGPD', 'CONFORMITE', 'Analyse de la conformité aux exigences du RGPD. Le système détecte 5 risques nécessitant une action immédiate.', 83, 'RSSI - Y. El Amrani', NOW(), 'Ce rapport évalue la conformité du système aux exigences du RGPD. Les données sensibles ont été identifiées et classifiées selon leur niveau de risque. Des recommandations sont fournies pour améliorer la conformité.'),
('Rapport de sécurité ISO 27001', 'INVENTAIRE', 'Évaluation de la sécurité de l''information selon les normes ISO 27001.', 78, 'RSSI - Y. El Amrani', NOW(), 'Ce rapport analyse la posture de sécurité de l''organisation selon les contrôles ISO 27001. Les contrôles d''accès et la gestion des données sensibles sont évalués.'),
('Rapport d''analyse des risques', 'RISQUES', 'Analyse détaillée des risques identifiés dans les documents.', 75, 'RSSI - Y. El Amrani', NOW(), 'Ce rapport présente une analyse complète des risques détectés dans les documents. Chaque risque est évalué selon sa gravité, sa probabilité et son impact.');

-- 5. Verify data insertion
SELECT 'Data insertion completed' as status;
SELECT COUNT(*) as documents_count FROM documents;
SELECT COUNT(*) as sensitive_data_count FROM sensitive_data;
SELECT COUNT(*) as risks_count FROM risks;
SELECT COUNT(*) as reports_count FROM reports;
