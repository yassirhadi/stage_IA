-- Update file paths to match actual file locations
-- Run this script to fix the file paths in the database

UPDATE documents SET file_path = 'D:\\Stage_IA_complet (2)\\sample-documents\\RH\\fiche_employe.txt' WHERE file_name = 'fiche_employe.txt';
UPDATE documents SET file_path = 'D:\\Stage_IA_complet (2)\\sample-documents\\Juridique\\contrat_travail.txt' WHERE file_name = 'contrat_travail.txt';
UPDATE documents SET file_path = 'D:\\Stage_IA_complet (2)\\sample-documents\\RH\\CV_Ahmed_Benali.txt' WHERE file_name = 'CV_Ahmed_Benali.txt';
UPDATE documents SET file_path = 'D:\\Stage_IA_complet (2)\\sample-documents\\facture_2025.txt' WHERE file_name = 'facture_2025.txt';
UPDATE documents SET file_path = 'D:\\Stage_IA_complet (2)\\sample-documents\\Finance\\budget_2025.txt' WHERE file_name = 'budget_2025.txt';
UPDATE documents SET file_path = 'D:\\Stage_IA_complet (2)\\sample-documents\\politique_ssi.txt' WHERE file_name = 'politique_ssi.txt';
UPDATE documents SET file_path = 'D:\\Stage_IA_complet (2)\\sample-documents\\Juridique\\Contrat_Prestation_2024.txt' WHERE file_name = 'Contrat_Prestation_2024.txt';
UPDATE documents SET file_path = 'D:\\Stage_IA_complet (2)\\sample-documents\\Finance\\Facture_2024_001.txt' WHERE file_name = 'Facture_2024_001.txt';
