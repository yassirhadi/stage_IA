-- Populate empty tables with realistic data
-- This file will be executed by Spring Boot on startup

-- Populate documents table first (required for sensitive_data foreign key)
INSERT IGNORE INTO documents (file_name, file_path, file_size, extension, document_type, confidentiality_level, analysis_status, created_at, updated_at) VALUES
('fiche_employe.txt', 'D:\\Stage_IA_complet (2)\\sample-documents\\RH\\fiche_employe.txt', 2048, 'txt', 'DOSSIER_RH', 'CONFIDENTIEL', 'COMPLETED', NOW(), NOW()),
('contrat_travail.txt', 'D:\\Stage_IA_complet (2)\\sample-documents\\Juridique\\contrat_travail.txt', 4096, 'txt', 'CONTRAT', 'CONFIDENTIEL', 'COMPLETED', NOW(), NOW()),
('CV_Ahmed_Benali.txt', 'D:\\Stage_IA_complet (2)\\sample-documents\\RH\\CV_Ahmed_Benali.txt', 1024, 'txt', 'DOSSIER_RH', 'INTERNE', 'COMPLETED', NOW(), NOW()),
('facture_2025.txt', 'D:\\Stage_IA_complet (2)\\sample-documents\\facture_2025.txt', 512, 'txt', 'FACTURE', 'TRES_CONFIDENTIEL', 'COMPLETED', NOW(), NOW()),
('budget_2025.txt', 'D:\\Stage_IA_complet (2)\\sample-documents\\Finance\\budget_2025.txt', 3072, 'txt', 'FACTURE', 'TRES_CONFIDENTIEL', 'COMPLETED', NOW(), NOW()),
('politique_ssi.txt', 'D:\\Stage_IA_complet (2)\\sample-documents\\politique_ssi.txt', 8192, 'txt', 'POLITIQUE_SSI', 'INTERNE', 'COMPLETED', NOW(), NOW()),
('Contrat_Prestation_2024.txt', 'D:\\Stage_IA_complet (2)\\sample-documents\\Juridique\\Contrat_Prestation_2024.txt', 2800, 'txt', 'CONTRAT', 'TRES_CONFIDENTIEL', 'COMPLETED', NOW(), NOW()),
('Facture_2024_001.txt', 'D:\\Stage_IA_complet (2)\\sample-documents\\Finance\\Facture_2024_001.txt', 1100, 'txt', 'FACTURE', 'CONFIDENTIEL', 'COMPLETED', NOW(), NOW());

-- Populate sensitive_data table with real data using subqueries (avoids hardcoded IDs)
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

-- Populate risks table with realistic data using subqueries (only fields that exist in Risk entity)
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

-- Populate reports table to avoid NaN and Invalid Date (only fields that exist in Report entity)
INSERT IGNORE INTO reports (title, report_type, executive_summary, overall_score, rssi_signature, generated_at, content) VALUES
('Rapport de conformité RGPD', 'CONFORMITE', 'Analyse de la conformité aux exigences du RGPD. Le système détecte 5 risques nécessitant une action immédiate.', 83, 'RSSI - Y. El Amrani', NOW(), 'Ce rapport évalue la conformité du système aux exigences du RGPD. Les données sensibles ont été identifiées et classifiées selon leur niveau de risque. Des recommandations sont fournies pour améliorer la conformité.'),
('Rapport de sécurité ISO 27001', 'INVENTAIRE', 'Évaluation de la sécurité de l''information selon les normes ISO 27001.', 78, 'RSSI - Y. El Amrani', NOW(), 'Ce rapport analyse la posture de sécurité de l''organisation selon les contrôles ISO 27001. Les contrôles d''accès et la gestion des données sensibles sont évalués.'),
('Rapport d''analyse des risques', 'RISQUES', 'Analyse détaillée des risques identifiés dans les documents.', 75, 'RSSI - Y. El Amrani', NOW(), 'Ce rapport présente une analyse complète des risques détectés dans les documents. Chaque risque est évalué selon sa gravité, sa probabilité et son impact.');

-- 1. Populate settings table
INSERT INTO settings (company_name, logo_url, language, timezone, email, storage_path, ocr_enabled) VALUES
('MaSociété SARL', '/uploads/logo.png', 'fr', 'Africa/Casablanca', 'support@masociete.ma', '/storage/documents', TRUE)
ON DUPLICATE KEY UPDATE company_name = company_name;

-- 2. Populate permissions table
INSERT INTO permissions (name, description) VALUES
('dashboard.view', 'Voir le tableau de bord'),
('users.read', 'Lire les utilisateurs'),
('users.create', 'Créer des utilisateurs'),
('users.update', 'Modifier les utilisateurs'),
('users.delete', 'Supprimer les utilisateurs'),
('documents.read', 'Lire les documents'),
('documents.upload', 'Uploader des documents'),
('documents.delete', 'Supprimer les documents'),
('documents.classify', 'Classifier les documents'),
('documents.analyze', 'Analyser les documents'),
('risks.read', 'Lire les risques'),
('risks.create', 'Créer des risques'),
('risks.update', 'Modifier les risques'),
('risks.delete', 'Supprimer les risques'),
('reports.read', 'Lire les rapports'),
('reports.generate', 'Générer des rapports'),
('reports.delete', 'Supprimer les rapports'),
('assistant.use', 'Utiliser l''assistant IA'),
('settings.manage', 'Gérer les paramètres'),
('notifications.read', 'Lire les notifications'),
('audit.view', 'Voir les logs d''audit'),
('roles.manage', 'Gérer les rôles'),
('folders.read', 'Lire les dossiers'),
('folders.create', 'Créer des dossiers'),
('folders.delete', 'Supprimer les dossiers'),
('assets.read', 'Lire les actifs'),
('assets.create', 'Créer des actifs'),
('assets.update', 'Modifier les actifs'),
('assets.delete', 'Supprimer les actifs'),
('referentials.read', 'Lire les référentiels'),
('recommendations.read', 'Lire les recommandations'),
('recommendations.manage', 'Gérer les recommandations')
ON DUPLICATE KEY UPDATE name = name;

-- 3. Get role IDs for role_permissions (using subqueries)
-- 4. Populate role_permissions table
-- ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMIN'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- RSSI gets document, risk, report, assistant permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'RSSI' AND p.name IN (
    'dashboard.view', 'documents.read', 'documents.upload', 'documents.delete', 
    'documents.classify', 'documents.analyze', 'risks.read', 'risks.create', 
    'risks.update', 'risks.delete', 'reports.read', 'reports.generate', 
    'assistant.use', 'notifications.read', 'folders.read', 'folders.create',
    'assets.read', 'referentials.read', 'recommendations.read', 'recommendations.manage'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- AUDITEUR gets read-only permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'AUDITEUR' AND p.name IN (
    'dashboard.view', 'documents.read', 'reports.read', 'referentials.read',
    'risks.read', 'folders.read', 'assets.read', 'audit.view', 'notifications.read'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- LECTEUR gets very limited permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'LECTEUR' AND p.name IN (
    'dashboard.view', 'documents.read', 'reports.read', 'notifications.read'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 5. Populate chats table (assuming user_id 1 exists)
INSERT INTO chats (user_id, title) VALUES
(1, 'Analyse ISO 27001'),
(1, 'Audit RGPD'),
(1, 'Classification documents RH'),
(1, 'Analyse des risques serveurs'),
(1, 'Documents confidentiels')
ON DUPLICATE KEY UPDATE title = title;

-- 6. Populate messages table
INSERT INTO messages (chat_id, sender, content, tokens_used) VALUES
((SELECT id FROM chats WHERE title = 'Analyse ISO 27001' LIMIT 1), 'USER', 'Quels sont les contrôles ISO 27001 pour l''accès aux données ?', 150),
((SELECT id FROM chats WHERE title = 'Analyse ISO 27001' LIMIT 1), 'ASSISTANT', 'Les contrôles ISO 27001 pour l''accès aux données incluent: 1) Contrôle d''accès utilisateur, 2) Gestion des droits d''accès, 3) Authentification forte, 4) Revue périodique des droits.', 320),
((SELECT id FROM chats WHERE title = 'Audit RGPD' LIMIT 1), 'USER', 'Analyse ce document pour la conformité RGPD', 180),
((SELECT id FROM chats WHERE title = 'Audit RGPD' LIMIT 1), 'ASSISTANT', 'Ce document contient des données personnelles (CIN, adresse). Il est conforme RGPD si vous avez le consentement des personnes concernées et des mesures de sécurité appropriées.', 280),
((SELECT id FROM chats WHERE title = 'Classification documents RH' LIMIT 1), 'USER', 'Comment classer les documents RH ?', 120),
((SELECT id FROM chats WHERE title = 'Classification documents RH' LIMIT 1), 'ASSISTANT', 'Les documents RH doivent être classés comme CONFIDENTIEL ou TRES_CONFIDENTIEL selon leur sensibilité. Ils nécessitent un contrôle d''accès strict.', 250),
((SELECT id FROM chats WHERE title = 'Analyse des risques serveurs' LIMIT 1), 'USER', 'Quels risques sont présents sur les serveurs ?', 140),
((SELECT id FROM chats WHERE title = 'Analyse des risques serveurs' LIMIT 1), 'ASSISTANT', 'Risques détectés: 1) Vulnérabilités non patchées, 2) Mots de passe faibles, 3) Accès non autorisé potentiel. Recommandation: Appliquer les patches et renforcer l''authentification.', 340),
((SELECT id FROM chats WHERE title = 'Documents confidentiels' LIMIT 1), 'USER', '5 documents contiennent un CIN', 100),
((SELECT id FROM chats WHERE title = 'Documents confidentiels' LIMIT 1), 'ASSISTANT', 'Effectivement, 5 documents contiennent des numéros CIN. Ces données sont sensibles et doivent être masquées ou chiffrées.', 220)
ON DUPLICATE KEY UPDATE content = content;

-- 7. Populate notifications table
INSERT INTO notifications (user_id, title, message, type, is_read, entity_type, entity_id) VALUES
(1, 'Analyse documentaire terminée', 'L''analyse de 15 documents a été terminée avec succès.', 'ANALYSIS', FALSE, 'document', NULL),
(1, 'Nouveau document importé', 'Le fichier "Contrat_Vendeur_2026.pdf" a été importé.', 'DOCUMENT', FALSE, 'document', NULL),
(1, 'Rapport généré', 'Le rapport de conformité ISO 27001 a été généré.', 'REPORT', FALSE, 'report', NULL),
(1, 'Risque critique détecté', 'Un risque critique a été détecté dans le dossier RH.', 'RISK', FALSE, 'risk', NULL),
(1, 'Score sécurité mis à jour', 'Le score de sécurité global est maintenant de 85%.', 'SYSTEM', TRUE, 'security_score', NULL),
(1, 'Classification terminée', '10 documents ont été classifiés automatiquement.', 'DOCUMENT', TRUE, 'document', NULL),
(1, 'Connexion administrateur', 'L''administrateur s''est connecté à 14:30.', 'SYSTEM', TRUE, 'user', 1),
(1, 'Nouvelle recommandation disponible', 'Une nouvelle recommandation pour le risque #12 est disponible.', 'RECOMMENDATION', FALSE, 'recommendation', NULL),
(1, 'Données sensibles détectées', '3 documents contiennent des données bancaires (IBAN).', 'RISK', FALSE, 'sensitive_data', NULL),
(1, 'Backup réussi', 'Le backup quotidien a été effectué avec succès.', 'SYSTEM', TRUE, 'backup', NULL)
ON DUPLICATE KEY UPDATE title = title;

-- 8. Populate recommendations table with real data
INSERT IGNORE INTO recommendations (risk_id, description, priority, status, assigned_to, deadline, progress, rssi_comment) VALUES
((SELECT id FROM risks WHERE title = 'Données bancaires exposées en clair' LIMIT 1), 'Masquer ou chiffrer les numéros IBAN dans les documents financiers', 'HAUTE', 'PROPOSEE', 1, DATE_ADD(NOW(), INTERVAL 7 DAY), 0, 'Action prioritaire requise'),
((SELECT id FROM risks WHERE title = 'CIN non protégé dans les documents RH' LIMIT 1), 'Appliquer le masquage automatique des numéros CIN dans les documents RH', 'MOYENNE', 'PROPOSEE', 1, DATE_ADD(NOW(), INTERVAL 14 DAY), 0, 'À traiter dans les 2 semaines'),
((SELECT id FROM risks WHERE title = 'Emails professionnels exposés' LIMIT 1), 'Restreindre l''accès aux documents contenant des emails professionnels', 'MOYENNE', 'EN_COURS', 1, DATE_ADD(NOW(), INTERVAL 10 DAY), 30, 'En cours de traitement'),
((SELECT id FROM risks WHERE title = 'Coordonnées bancaires non sécurisées' LIMIT 1), 'Chiffrer les documents contenant des informations bancaires', 'HAUTE', 'PROPOSEE', 1, DATE_ADD(NOW(), INTERVAL 5 DAY), 0, 'Urgent'),
((SELECT id FROM risks WHERE title = 'Numéros de téléphone visibles' LIMIT 1), 'Masquer partiellement les numéros de téléphone dans les documents', 'BASSE', 'PROPOSEE', 1, DATE_ADD(NOW(), INTERVAL 30 DAY), 0, 'Peut être traité plus tard');
