# Guide de démarrage du backend

## 1. Prérequis

- Python 3.11 ou plus récent
- Java 17 et Maven pour le backend Spring Boot
- MySQL et Node.js

## 2. Démarrer le service IA Python

Ouvrez PowerShell à la racine de votre copie du projet. Pour activer le rechargement automatique en développement, ajoutez l’option `--reload` à la commande.

```powershell
python start_backend.py --host 127.0.0.1 --port 8000
```

La documentation OpenAPI est disponible sur http://127.0.0.1:8000/docs. Pour vérifier le service, ouvrez http://127.0.0.1:8000/api/v1/health.

## 3. Démarrer le backend Java

Dans PowerShell, vérifiez Maven avec `mvn --version`. Définissez `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` et `JWT_SECRET` avec les valeurs de votre installation.

Depuis la racine du projet, exécutez ces commandes.

```powershell
cd Backend_IA
mvn spring-boot:run
```

Avec IntelliJ IDEA ou Eclipse, ouvrez `Backend_IA` et lancez `CopilotRssiApplication`. Pour vérifier le backend, ouvrez http://localhost:8080/api/health.

## 4. Démarrer le frontend

Ouvrez un nouveau terminal PowerShell à la racine du projet.

```powershell
cd Frontend_IA
npm install
npm run dev
```

Ouvrez http://localhost:5173 dans votre navigateur.

## Configurer les comptes de connexion

Renseignez les trois variables du compte à créer avant de lancer le backend Java.

| Compte | Nom d’utilisateur | Mot de passe | Adresse e-mail |
| --- | --- | --- | --- |
| Administrateur | `BOOTSTRAP_ADMIN_USERNAME` | `BOOTSTRAP_ADMIN_PASSWORD` | `BOOTSTRAP_ADMIN_EMAIL` |
| RSSI | `BOOTSTRAP_RSSI_USERNAME` | `BOOTSTRAP_RSSI_PASSWORD` | `BOOTSTRAP_RSSI_EMAIL` |

## Dépannage

- Si le service IA affiche `ModuleNotFoundError: No module named 'app'`, lancez `python start_backend.py` à la racine du projet.
- Si une page blanche apparaît après la connexion, vérifiez que le backend Java répond sur http://localhost:8080/api/health.
- Si le backend ne démarre pas, vérifiez que le port 8080 est libre.
- Si MySQL refuse la connexion, vérifiez les identifiants et assurez-vous que `DB_URL` et `MYSQL_DATABASE` désignent la base que vous avez créée.
