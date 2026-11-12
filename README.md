# faro

Application web **Java (Spring Boot) + React**, avec authentification **OAuth2 / OpenID Connect** et base **PostgreSQL**.

| Partie | Techno | Port local |
|---|---|---|
| `backend/` | Java 21, Spring Boot 4, Spring Security (resource server JWT), JPA, Flyway | 8080 |
| `frontend/` | React 19, TypeScript, Vite, `react-oidc-context` (Authorization Code + PKCE) | 5173 |
| Fournisseur OIDC | Keycloak (Docker), realm `faro` | 8180 |
| Base de données | PostgreSQL 17 (Docker) | 5432 |

## Fonctionnalités

- **Affaires** : client, pièces avec métrés (sol, murs, périmètre), statut de la visite au chantier.
- **Visite** : notes et photos par pièce (sur mobile), compte-rendu PDF.
- **Estimation rapide** en fourchette (ratios €/m² par type de pièce, recalés sur les devis).
- **Devis pièces × lots** : chaque ouvrage saisi une fois, vue par pièce (client) ou par lot (chantier), descriptifs,
  options Eco / Standard / Premium, bibliothèque de prix (import / export CSV Excel), PDF, export CSV.
- **Versions figées** du devis et **passage en chantier** (dossier travaux de référence).
- **Avenants** : brouillon → envoyé → validé / refusé, montant du marché.
- **Marge** prévue (déboursé sec) contre réelle (dépenses), par lot ; **bons de commande** par lot.
- **Espace client** par lien secret : devis, choix des finitions, validation des avenants.
- **Équipe** : gérant, chargé d'études, conducteur de travaux, invitations ; **offres** et page d'administration du SaaS.
- **Module investisseur** : bilan d'opération, rentabilité, TRI, scénarios.

## Tout lancer en une commande

Avec Docker (Docker Desktop, ou Docker dans WSL sous Windows), depuis le dossier du projet :

```bash
docker compose --profile complet up -d --build
```

Puis ouvrir **http://localhost:3000** et se connecter avec `demo` / `demo` (ou `admin` / `admin`).

Sur une base neuve, des **données d'exemple** sont chargées : l'entreprise « Rénov Lyon (démo) », 21 ouvrages
dans la bibliothèque et 7 affaires à tous les stades (visite, estimation, devis avec options Eco / Standard / Premium,
chantiers avec avenants et dépenses, opération de marchand de biens, affaire perdue). Pour repartir de zéro :
`docker compose --profile complet down -v` puis relancer.
Le premier lancement prend quelques minutes (construction des images). Pour tout arrêter : `docker compose --profile complet down`
(ajouter `-v` pour effacer aussi les données).

Après un `git pull`, relancer la même commande : les images sont reconstruites avec le nouveau code.

### Depuis les autres appareils du wifi

Voir [`docs/reseau-local.md`](docs/reseau-local.md) : faro en HTTPS sur `https://IP-DU-PC` (téléphone, tablette).

## Lancer en local (développement)

Prérequis : Docker, Java 21, Node.js 22 (Maven est fourni par `backend/mvnw`).

```bash
# 1. PostgreSQL + Keycloak seulement (le realm faro est importé automatiquement)
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
cd backend && ./mvnw verify      # base dédiée faro_test (vidée par les tests), voir ci-dessous
cd frontend && npm test
```

Les tests backend utilisent la base `faro_test`, créée automatiquement par `docker compose` sur un volume neuf.
Sur un volume existant : `docker compose exec postgres createdb -U faro faro_test`.
Sans Docker : `CREATE DATABASE faro_test OWNER faro;`.

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

## Mise en production

Voir [`docs/deploiement.md`](docs/deploiement.md) : un serveur, Docker, deux noms de domaine, HTTPS automatique.

## Suivi du projet

L'avancement est suivi dans [`PROGRESS.md`](PROGRESS.md) ; [`CLAUDE.md`](CLAUDE.md) explique à Claude comment reprendre le travail d'une session à l'autre.
