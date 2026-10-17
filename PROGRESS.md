# Avancement du projet faro

> Fichier de suivi lu au début de chaque session Claude (voir `CLAUDE.md`).
> Dernière mise à jour : 2026-10-17

## Objectif du projet

Application web Java + React, authentification OAuth2 / OpenID Connect, base PostgreSQL.
Objectif fonctionnel : _à préciser avec l'utilisateur._

## Stack

- Backend : Java 21, Spring Boot 4.1, Maven, Spring Security (resource server JWT), Spring Data JPA, Flyway
- Frontend : React + TypeScript + Vite, connexion OIDC avec Authorization Code + PKCE
- Fournisseur OIDC : Keycloak en local (Docker), remplaçable par un autre fournisseur OIDC
- Base de données : PostgreSQL (Docker)
- Environnement local : `docker-compose.yml` (Postgres + Keycloak)
- Structure : monorepo `backend/` + `frontend/`

## En cours

- Phase 1 terminée. En attente de l'objectif fonctionnel pour découper la phase 2.

## À faire

- [ ] Définir l'objectif fonctionnel et le périmètre avec l'utilisateur
- [ ] Phase 1 : socle technique
  - [x] `docker-compose.yml` avec PostgreSQL et Keycloak (realm `faro`, client `faro-frontend`, utilisateurs `demo/demo` et `admin/admin`)
  - [x] Backend Spring Boot : squelette, connexion Postgres, Flyway, endpoint `/api/health`
  - [x] Backend : sécurité resource server JWT, endpoint protégé `/api/me` (enregistre l'utilisateur en base)
  - [x] Frontend React/Vite : squelette, connexion/déconnexion OIDC (PKCE) avec `react-oidc-context`
  - [x] Frontend : appel de `/api/me` avec le jeton, affichage de l'utilisateur connecté
  - [x] Test de bout en bout manuel (Keycloak 26.3 + backend + Vite + Playwright) : connexion, `/api/me` 200, utilisateur en base, déconnexion
  - [x] README : comment lancer le projet en local
  - [x] CI GitHub Actions : build et tests backend (avec service PostgreSQL) + frontend
- [ ] Phase 2 : fonctionnalités métier (à découper une fois l'objectif défini)

## Fait

- [x] Mettre en place le système de reprise automatique (`CLAUDE.md` + `PROGRESS.md`)

## Décisions

- Le suivi de l'avancement se fait dans `PROGRESS.md`, mis à jour et poussé à chaque étape.
- Stack demandée par l'utilisateur : Java, React, OAuth2/OIDC, PostgreSQL. Spring Boot confirmé.
- Package Java racine : `com.faro`. Spring Boot 4 / Spring Security 7 (attention : starters modulaires, ex. `spring-boot-starter-webmvc`, et autorité `FACTOR_BEARER` ajoutée aux jetons).
- Rôles : claim `roles` du jeton (mapper Keycloak des rôles du realm) → `ROLE_xxx`. `/api/admin/**` réservé au rôle `admin`.
- Audience attendue dans le jeton : `faro-api` (mapper d'audience Keycloak sur le client `faro-frontend`).
- Keycloak écoute sur le port 8180, le backend sur 8080, le frontend sur 5173.
- Frontend : React 19, Vite 8, TypeScript 7, tests Vitest + Testing Library. Config OIDC via `VITE_OIDC_AUTHORITY`, `VITE_OIDC_CLIENT_ID`, `VITE_API_URL` (voir `frontend/.env.example`). Jetons stockés en `sessionStorage`.
- Dans une session cloud sans Docker : Keycloak peut être lancé depuis le zip officiel (`bin/kc.sh start-dev --import-realm --http-port=8180`) et PostgreSQL 16 est installé localement (`service postgresql start`).
- Les tests backend sont des tests d'intégration sur un vrai PostgreSQL (base `faro`, utilisateur `faro/faro`), pas de Docker requis pour les tests.

## Bloquants / questions

- Quel est l'objectif fonctionnel de faro ? (utilisateurs, fonctionnalités principales)
- Un fournisseur OIDC est-il imposé en production (Google, Entra, Auth0…) ? Keycloak par défaut en attendant.
