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

- Frontend React/Vite (dossier `frontend/`).

## À faire

- [ ] Définir l'objectif fonctionnel et le périmètre avec l'utilisateur
- [ ] Phase 1 : socle technique
  - [x] `docker-compose.yml` avec PostgreSQL et Keycloak (realm `faro`, client `faro-frontend`, utilisateurs `demo/demo` et `admin/admin`)
  - [x] Backend Spring Boot : squelette, connexion Postgres, Flyway, endpoint `/api/health`
  - [x] Backend : sécurité resource server JWT, endpoint protégé `/api/me` (enregistre l'utilisateur en base)
  - [ ] Frontend React/Vite : squelette, connexion/déconnexion OIDC (PKCE)
  - [ ] Frontend : appel de `/api/me` avec le jeton, affichage de l'utilisateur connecté
  - [ ] README : comment lancer le projet en local
  - [ ] CI GitHub Actions : build et tests backend + frontend
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
- Les tests backend sont des tests d'intégration sur un vrai PostgreSQL (base `faro`, utilisateur `faro/faro`), pas de Docker requis pour les tests.

## Bloquants / questions

- Quel est l'objectif fonctionnel de faro ? (utilisateurs, fonctionnalités principales)
- Un fournisseur OIDC est-il imposé en production (Google, Entra, Auth0…) ? Keycloak par défaut en attendant.
- Keycloak n'a pas pu être lancé dans la session cloud (pas de Docker) : la connexion de bout en bout est à vérifier en local.
