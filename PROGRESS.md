# Avancement du projet faro

> Fichier de suivi lu au début de chaque session Claude (voir `CLAUDE.md`).
> Dernière mise à jour : 2026-10-17

## Objectif du projet

Application web Java + React, authentification OAuth2 / OpenID Connect, base PostgreSQL.
Objectif fonctionnel : _à préciser avec l'utilisateur._

## Stack (proposée, à confirmer)

- Backend : Java 21, Spring Boot 3, Maven, Spring Security (resource server JWT), Spring Data JPA, Flyway
- Frontend : React + TypeScript + Vite, connexion OIDC avec Authorization Code + PKCE
- Fournisseur OIDC : Keycloak en local (Docker), remplaçable par un autre fournisseur OIDC
- Base de données : PostgreSQL (Docker)
- Environnement local : `docker-compose.yml` (Postgres + Keycloak)
- Structure : monorepo `backend/` + `frontend/`

## En cours

- _Rien pour l'instant._

## À faire

- [ ] Définir l'objectif fonctionnel et le périmètre avec l'utilisateur
- [ ] Confirmer la stack proposée
- [ ] Phase 1 : socle technique
  - [ ] `docker-compose.yml` avec PostgreSQL et Keycloak (realm, client front, utilisateur de test importés)
  - [ ] Backend Spring Boot : squelette, connexion Postgres, Flyway, endpoint `/api/health`
  - [ ] Backend : sécurité resource server JWT validé par Keycloak, endpoint protégé `/api/me`
  - [ ] Frontend React/Vite : squelette, connexion/déconnexion OIDC (PKCE)
  - [ ] Frontend : appel de `/api/me` avec le jeton, affichage de l'utilisateur connecté
  - [ ] README : comment lancer le projet en local
  - [ ] CI GitHub Actions : build et tests backend + frontend
- [ ] Phase 2 : fonctionnalités métier (à découper une fois l'objectif défini)

## Fait

- [x] Mettre en place le système de reprise automatique (`CLAUDE.md` + `PROGRESS.md`)

## Décisions

- Le suivi de l'avancement se fait dans `PROGRESS.md`, mis à jour et poussé à chaque étape.
- Stack demandée par l'utilisateur : Java, React, OAuth2/OIDC, PostgreSQL.

## Bloquants / questions

- Quel est l'objectif fonctionnel de faro ? (utilisateurs, fonctionnalités principales)
- La stack proposée (Spring Boot, Maven, Vite, Keycloak) convient-elle ? Un fournisseur OIDC est-il déjà imposé (Google, Azure AD/Entra, Auth0…) ?
