
# Copilot RSSI – Plateforme intelligente d'assistance au Responsable de la Sécurité des Systèmes d'Information

Projet de stage – Architecture modulaire basée sur Spring Boot, React.js, MySQL et FastAPI.

## Architecture

```text
Frontend_IA (React.js) → Backend_IA (Spring Boot) → MySQL
                                  |
                                  v
                           IA (FastAPI/Python)
```

## Structure du projet

```text
Stage_IA/
├── Backend_IA/          # API REST Spring Boot (Java 17)
├── Frontend_IA/         # Interface React.js (Vite + TypeScript)
├── IA/                  # Microservice IA Python (FastAPI)
├── database/            # Scripts SQL MySQL
├── sample-documents/    # Documents de test fictifs
└── README.md
```

## Prérequis

- Java JDK 17+
- Node.js 18+
- Python 3.11+
- MySQL 8+
- Maven
- IntelliJ IDEA ou VS Code

## Démarrage rapide

### 1. Configuration de la base de données

Créer ou importer la base de données MySQL à partir du fichier :

`database/schema.sql`

Configurer les variables d'environnement suivantes :

- `DB_URL` : URL de connexion MySQL
- `DB_USERNAME` : nom d'utilisateur MySQL
- `DB_PASSWORD` : mot de passe MySQL

Exemple de configuration dans `application-dev.yml` :

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

logging:
  level:
    com.copilot.rssi: DEBUG
```

Les informations sensibles de connexion sont gérées par des variables d'environnement et ne doivent pas être publiées dans le dépôt GitHub.

### 2. Backend Spring Boot

Ouvrir le dossier `Backend_IA` dans IntelliJ IDEA.

Configurer les variables d'environnement, puis exécuter la classe principale :

`CopilotRssiApplication`

Services accessibles :

- API REST : http://localhost:8080/api
- Swagger UI : http://localhost:8080/api/swagger-ui.html

### Comptes de test

Deux rôles sont disponibles :

- **ADMIN** : administration et gestion de la plateforme.
- **RSSI** : inventaire documentaire, analyse des données sensibles et gestion des risques.

Les identifiants de test sont fournis séparément.

### 3. Service IA Python

Ouvrir un terminal à la racine du projet :

```powershell
cd IA
python -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
python run.py
```

Services accessibles :

- API IA : http://localhost:8000
- Documentation FastAPI : http://localhost:8000/docs

### 4. Frontend React.js

Ouvrir un autre terminal à la racine du projet :

```powershell
cd Frontend_IA
npm install
npm run dev
```

Interface accessible :

http://localhost:5173

## Fonctionnalités implémentées

### Authentification et sécurité

- [x] Authentification JWT
- [x] Gestion des rôles ADMIN et RSSI
- [x] Protection des routes
- [x] Chiffrement des mots de passe avec BCrypt
- [x] Validation des données

### Inventaire et gestion documentaire

- [x] Inventaire automatique des fichiers
- [x] Gestion des actifs informatiques
- [x] Extraction de contenu PDF, Word, Excel et TXT
- [x] Reconnaissance OCR des images
- [x] Classification automatique des documents

### Intelligence artificielle

- [x] Analyse intelligente des documents
- [x] Détection des données sensibles
- [x] Identification des CIN, emails et téléphones
- [x] Détection des IBAN et mots de passe
- [x] Analyse et évaluation des risques
- [x] Génération de recommandations
- [x] Copilote IA pour les questions et réponses

### Interface utilisateur

- [x] Interface React.js avec TypeScript
- [x] Navigation entre les modules
- [x] Tableaux de bord
- [x] Gestion des documents et des actifs
- [x] Consultation des analyses et des risques

## Test du flux complet

1. Démarrer MySQL.
2. Lancer le backend Spring Boot.
3. Démarrer le microservice IA FastAPI.
4. Lancer le frontend React.js.
5. Se connecter avec un compte RSSI de test.
6. Accéder au module **Inventaire**.
7. Scanner le dossier `sample-documents` du projet.
8. Cliquer sur **Analyser IA** pour un document.
9. Consulter les résultats de l'analyse.
10. Accéder aux modules **Risques** et **Copilote IA**.

## Bonnes pratiques appliquées

### Backend

- Architecture en couches : Controller, Service, Repository
- API REST
- DTO et validation des données
- Authentification JWT
- Gestion globale des exceptions
- Documentation Swagger
- Principes SOLID

### Frontend

- Composants React réutilisables
- TypeScript
- Context API
- Routes protégées
- Services API
- Interface responsive

### Intelligence artificielle

- Microservice Python FastAPI
- Modules indépendants d'extraction
- Classification documentaire
- Détection des données sensibles
- Analyse des risques
- Communication REST avec Spring Boot

## Sécurité et confidentialité

- Les identifiants de connexion sont externalisés.
- Les mots de passe ne doivent pas être publiés sur GitHub.
- Les fichiers `.env` contenant des secrets doivent être exclus du versionnement.
- Les documents de test doivent contenir uniquement des données fictives.
- Les accès aux fonctionnalités sont contrôlés selon les rôles.

## Perspectives d'amélioration

- Génération de rapports PDF et Excel
- Intégration de modèles LLM locaux
- Amélioration du Copilote IA avec RAG
- Exploitation des référentiels ISO 27001 et NIST
- Renforcement des tests unitaires et d'intégration
- Amélioration de l'importation des documents
- Optimisation des performances de l'analyse IA

## Technologies utilisées

| Domaine | Technologies |
|---------|--------------|
| Backend | Java 17, Spring Boot, Spring Security |
| Frontend | React.js, TypeScript, Vite |
| Intelligence artificielle | Python, FastAPI, NLP, OCR |
| Base de données | MySQL |
| Sécurité | JWT, BCrypt |
| API | REST, Swagger |
| Outils | Git, GitHub, IntelliJ IDEA, VS Code |

## Contexte du projet

Copilot RSSI est une plateforme intelligente développée dans le cadre d'un stage à la Fondation des Œuvres Sociales du personnel du Ministère de l'Économie et des Finances (FOSMEF).

Le projet vise à faciliter le travail du Responsable de la Sécurité des Systèmes d'Information (RSSI) grâce à l'automatisation de l'inventaire documentaire, de la détection des données sensibles et de l'analyse des risques.

L'intelligence artificielle intervient comme outil d'assistance à la décision, tandis que le RSSI conserve la responsabilité des décisions de sécurité.
