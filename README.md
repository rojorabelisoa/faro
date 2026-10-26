# faro

Application web **Java (Spring Boot) + React**, avec authentification **OAuth2 / OpenID Connect** et base **PostgreSQL**.

| Partie | Techno | Port local |
|---|---|---|
| `backend/` | Java 21, Spring Boot 4, Spring Security (resource server JWT), JPA, Flyway | 8080 |
| `frontend/` | React 19, TypeScript, Vite, `react-oidc-context` (Authorization Code + PKCE) | 5173 |
| Fournisseur OIDC | Keycloak (Docker), realm `faro` | 8180 |
| Base de données | PostgreSQL 17 (Docker) | 5432 |

## Lancer en local

Prérequis : Docker, Java 21, Node.js 22 (Maven est fourni par `backend/mvnw`).

```bash
# 1. PostgreSQL + Keycloak (le realm faro est importé automatiquement)
docker compose up -d

# 2. Backend (http://localhost:8080)
cd backend
./mvnw spring-boot:run

# 3. Frontend (http://localhost:5173), dans un autre terminal
cd frontend
npm install
npm run dev
```

Ouvrir http://localhost:5173 puis « Se connecter ». Comptes de test :

| Utilisateur | Mot de passe | Rôles |
|---|---|---|
| `demo` | `demo` | `user` |
| `admin` | `admin` | `user`, `admin` |

Console d'administration Keycloak : http://localhost:8180 (`admin` / `admin`).

## Lancer sous Windows sans Docker

Prérequis (PowerShell) :

```powershell
winget install -e --id EclipseAdoptium.Temurin.21.JDK
winget install -e --id OpenJS.NodeJS.LTS
winget install -e --id PostgreSQL.PostgreSQL.17
```

Maven n'est pas nécessaire : le projet contient `backend\mvnw.cmd`.

1. **PostgreSQL** : créer l'utilisateur et la base (mot de passe `postgres` choisi à l'installation) :
   ```powershell
   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -c "CREATE USER faro WITH PASSWORD 'faro';" -c "CREATE DATABASE faro OWNER faro;"
   ```
2. **Keycloak** : télécharger `keycloak-26.3.5.zip` (https://github.com/keycloak/keycloak/releases), le dézipper
   (ex. `D:\outils\keycloak`), copier `keycloak\faro-realm.json` dans `D:\outils\keycloak\data\import\`, puis :
   ```powershell
   cd D:\outils\keycloak
   $env:KC_BOOTSTRAP_ADMIN_USERNAME="admin"; $env:KC_BOOTSTRAP_ADMIN_PASSWORD="admin"
   bin\kc.bat start-dev --import-realm --http-port=8180
   ```
3. **Backend** (nouveau terminal) : `cd backend` puis `.\mvnw.cmd spring-boot:run`
4. **Frontend** (nouveau terminal) : `cd frontend`, `npm install`, `npm run dev`

## Tests

```bash
cd backend && ./mvnw verify      # nécessite PostgreSQL démarré (docker compose up -d postgres)
cd frontend && npm test
```

## API

| Méthode | Route | Accès |
|---|---|---|
| GET | `/api/health` | public |
| GET | `/api/me` | connecté — renvoie l'utilisateur et l'enregistre en base |
| CRUD | `/api/affaires` | affaires de l'entreprise |
| CRUD | `/api/affaires/{id}/pieces` | pièces d'une affaire |
| CRUD | `/api/lots` | lots / corps d'état (14 créés par défaut) |
| CRUD | `/api/ouvrages?q=&lotId=` | bibliothèque d'ouvrages |
| GET | `/api/affaires/{id}/devis` | lignes + totaux par pièce, par lot, par TVA |
| POST/PUT/DELETE | `/api/affaires/{id}/lignes` | lignes de devis (`/lignes/depuis-ouvrage` pour copier un ouvrage) |
| * | `/api/admin/**` | rôle `admin` |

Toutes les routes métier sont cloisonnées par entreprise (organisation) de l'utilisateur connecté.

## Configuration

Backend (variables d'environnement, valeurs par défaut pour le local) :

| Variable | Défaut |
|---|---|
| `FARO_DB_URL` | `jdbc:postgresql://localhost:5432/faro` |
| `FARO_DB_USER` / `FARO_DB_PASSWORD` | `faro` / `faro` |
| `FARO_OIDC_ISSUER_URI` | `http://localhost:8180/realms/faro` |
| `FARO_OIDC_AUDIENCE` | `faro-api` |
| `FARO_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |

Frontend : voir `frontend/.env.example` (`VITE_OIDC_AUTHORITY`, `VITE_OIDC_CLIENT_ID`, `VITE_API_URL`).

Pour utiliser un autre fournisseur OIDC (Google, Entra ID, Auth0…), changer l'issuer et le client ID ; le fournisseur doit émettre un jeton d'accès JWT avec l'audience attendue et, pour les rôles, une claim `roles`.

## Suivi du projet

L'avancement est suivi dans [`PROGRESS.md`](PROGRESS.md) ; [`CLAUDE.md`](CLAUDE.md) explique à Claude comment reprendre le travail d'une session à l'autre.
