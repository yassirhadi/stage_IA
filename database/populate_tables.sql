-- Populate empty tables with realistic data

USE copilot_rssi;

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
('users.delete', 'Supprimer des utilisateurs'),
('documents.read', 'Lire les documents'),
('documents.upload', 'Uploader des documents'),
('documents.delete', 'Supprimer des documents'),
('documents.classify', 'Classifier les documents'),
('documents.analyze', 'Analyser les documents'),
('risks.read', 'Lire les risques'),
('risks.create', 'Créer des risques'),
('risks.update', 'Modifier les risques'),
('risks.delete', 'Supprimer des risques'),
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
('folders.delete', 'Supprimer des dossiers'),
('assets.read', 'Lire les actifs'),
('assets.create', 'Créer des actifs'),
('assets.update', 'Modifier les actifs'),
('assets.delete', 'Supprimer les actifs'),
('referentials.read', 'Lire les référentiels'),
('recommendations.read', 'Lire les recommandations'),
('recommendations.manage', 'Gérer les recommandations')
ON DUPLICATE KEY UPDATE name = name;

-- 3. Get role IDs for role_permissions
SET @admin_id = (SELECT id FROM roles WHERE name = 'ADMIN' LIMIT 1);
SET @rssi_id = (SELECT id FROM roles WHERE name = 'RSSI' LIMIT 1);
SET @auditeur_id = (SELECT id FROM roles WHERE name = 'AUDITEUR' LIMIT 1);
SET @lecteur_id = (SELECT id FROM roles WHERE name = 'LECTEUR' LIMIT 1);

-- 4. Populate role_permissions table
-- ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT @admin_id, id FROM permissions
ON DUPLICATE KEY UPDATE role_id = role_id;

-- RSSI gets document, risk, report, assistant permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT @rssi_id, id FROM permissions 
WHERE name IN (
    'dashboard.view', 'documents.read', 'documents.upload', 'documents.delete', 
    'documents.classify', 'documents.analyze', 'risks.read', 'risks.create', 
    'risks.update', 'risks.delete', 'reports.read', 'reports.generate', 
    'assistant.use', 'notifications.read', 'folders.read', 'folders.create',
    'assets.read', 'referentials.read', 'recommendations.read', 'recommendations.manage'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- AUDITEUR gets read-only permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT @auditeur_id, id FROM permissions 
WHERE name IN (
    'dashboard.view', 'documents.read', 'reports.read', 'referentials.read',
    'risks.read', 'folders.read', 'assets.read', 'audit.view', 'notifications.read'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- LECTEUR gets very limited permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT @lecteur_id, id FROM permissions 
WHERE name IN (
    'dashboard.view', 'documents.read', 'reports.read', 'notifications.read'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 5. Populate chats table (assuming user_id 1 exists)
SET @user_id = 1;
INSERT INTO chats (user_id, title) VALUES
(@user_id, 'Analyse ISO 27001'),
(@user_id, 'Audit RGPD'),
(@user_id, 'Classification documents RH'),
(@user_id, 'Analyse des risques serveurs'),
(@user_id, 'Documents confidentiels')
ON DUPLICATE KEY UPDATE title = title;

-- 6. Populate messages table
SET @chat1_id = (SELECT id FROM chats WHERE title = 'Analyse ISO 27001' LIMIT 1);
SET @chat2_id = (SELECT id FROM chats WHERE title = 'Audit RGPD' LIMIT 1);
SET @chat3_id = (SELECT id FROM chats WHERE title = 'Classification documents RH' LIMIT 1);
SET @chat4_id = (SELECT id FROM chats WHERE title = 'Analyse des risques serveurs' LIMIT 1);
SET @chat5_id = (SELECT id FROM chats WHERE title = 'Documents confidentiels' LIMIT 1);

INSERT INTO messages (chat_id, sender, content, tokens_used) VALUES
(@chat1_id, 'USER', 'Quels sont les contrôles ISO 27001 pour l''accès aux données ?', 150),
(@chat1_id, 'ASSISTANT', 'Les contrôles ISO 27001 pour l''accès aux données incluent: 1) Contrôle d''accès utilisateur, 2) Gestion des droits d''accès, 3) Authentification forte, 4) Revue périodique des droits.', 320),
(@chat2_id, 'USER', 'Analyse ce document pour la conformité RGPD', 180),
(@chat2_id, 'ASSISTANT', 'Ce document contient des données personnelles (CIN, adresse). Il est conforme RGPD si vous avez le consentement des personnes concernées et des mesures de sécurité appropriées.', 280),
(@chat3_id, 'USER', 'Comment classer les documents RH ?', 120),
(@chat3_id, 'ASSISTANT', 'Les documents RH doivent être classés comme CONFIDENTIEL ou TRES_CONFIDENTIEL selon leur sensibilité. Ils nécessitent un contrôle d''accès strict.', 250),
(@chat4_id, 'USER', 'Quels risques sont présents sur les serveurs ?', 140),
(@chat4_id, 'ASSISTANT', 'Risques détectés: 1) Vulnérabilités non patchées, 2) Mots de passe faibles, 3) Accès non autorisé potentiels. Recommandation: Appliquer les patches et renforcer l''authentification.', 340),
(@chat5_id, 'USER', '5 documents contiennent un CIN', 100),
(@chat5_id, 'ASSISTANT', 'Effectivement, 5 documents contiennent des numéros CIN. Ces données sont sensibles et doivent être masquées ou chiffrées.', 220)
ON DUPLICATE KEY UPDATE content = content;

-- 7. Populate notifications table
INSERT INTO notifications (user_id, title, message, type, is_read, entity_type, entity_id) VALUES
(@user_id, 'Analyse documentaire terminée', 'L''analyse de 15 documents a été terminée avec succès.', 'ANALYSIS', FALSE, 'document', NULL),
(@user_id, 'Nouveau document importé', 'Le fichier "Contrat_Vendeur_2026.pdf" a été importé.', 'DOCUMENT', FALSE, 'document', NULL),
(@user_id, 'Rapport généré', 'Le rapport de conformité ISO 27001 a été généré.', 'REPORT', FALSE, 'report', NULL),
(@user_id, 'Risque critique détecté', 'Un risque critique a été détecté dans le dossier RH.', 'RISK', FALSE, 'risk', NULL),
(@user_id, 'Score sécurité mis à jour', 'Le score de sécurité global est maintenant de 85%.', 'SYSTEM', TRUE, 'security_score', NULL),
(@user_id, 'Classification terminée', '10 documents ont été classifiés automatiquement.', 'DOCUMENT', TRUE, 'document', NULL),
(@user_id, 'Connexion administrateur', 'L''administrateur s''est connecté à 14:30.', 'SYSTEM', TRUE, 'user', @user_id),
(@user_id, 'Nouvelle recommandation disponible', 'Une nouvelle recommandation pour le risque #12 est disponible.', 'RECOMMENDATION', FALSE, 'recommendation', NULL),
(@user_id, 'Données sensibles détectées', '3 documents contiennent des données bancaires (IBAN).', 'RISK', FALSE, 'sensitive_data', NULL),
(@user_id, 'Backup réussi', 'Le backup quotidien a été effectué avec succès.', 'SYSTEM', TRUE, 'backup', NULL)
ON DUPLICATE KEY UPDATE title = title;
