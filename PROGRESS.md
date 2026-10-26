# Avancement du projet faro

> Fichier de suivi lu au début de chaque session Claude (voir `CLAUDE.md`).
> Dernière mise à jour : 2026-10-17

## Objectif du projet

Application web Java + React, authentification OAuth2 / OpenID Connect, base PostgreSQL.
Objectif fonctionnel : **logiciel de pilotage intelligent de projets de construction / rénovation**
(cockpit budget + planning + décisions/avenants + preuves, pour MOE/économistes, architectes, clients, investisseurs).
Voir `docs/business-plan-v1.0.pdf` et `docs/brainstorm.md`.

## Stack

- Backend : Java 21, Spring Boot 4.1, Maven, Spring Security (resource server JWT), Spring Data JPA, Flyway
- Frontend : React + TypeScript + Vite, connexion OIDC avec Authorization Code + PKCE
- Fournisseur OIDC : Keycloak en local (Docker), remplaçable par un autre fournisseur OIDC
- Base de données : PostgreSQL (Docker)
- Environnement local : `docker-compose.yml` (Postgres + Keycloak)
- Structure : monorepo `backend/` + `frontend/`

## En cours

- 2.5 Frontend : liste et création des affaires (dossier `frontend/src`, ajout d'un routeur).
- En parallèle : entretien métier non terminé (questions de suivi dans `docs/brainstorm.md`). L'utilisateur a demandé de commencer sans attendre.

## À faire

- [ ] Brainstorm : définir le périmètre du MVP avec l'utilisateur (`docs/brainstorm.md`)
- [ ] Phase 1 : socle technique
  - [x] `docker-compose.yml` avec PostgreSQL et Keycloak (realm `faro`, client `faro-frontend`, utilisateurs `demo/demo` et `admin/admin`)
  - [x] Backend Spring Boot : squelette, connexion Postgres, Flyway, endpoint `/api/health`
  - [x] Backend : sécurité resource server JWT, endpoint protégé `/api/me` (enregistre l'utilisateur en base)
  - [x] Frontend React/Vite : squelette, connexion/déconnexion OIDC (PKCE) avec `react-oidc-context`
  - [x] Frontend : appel de `/api/me` avec le jeton, affichage de l'utilisateur connecté
  - [x] Test de bout en bout manuel (Keycloak 26.3 + backend + Vite + Playwright) : connexion, `/api/me` 200, utilisateur en base, déconnexion
  - [x] README : comment lancer le projet en local
  - [x] CI GitHub Actions : build et tests backend (avec service PostgreSQL) + frontend
- [ ] Phase 2 : MVP « devis pièces × lots » (cible : entreprises de rénovation, voir `docs/brainstorm.md`)
  - [x] 2.1 Backend : organisation (entreprise) créée à la première connexion, affaires (CRUD) cloisonnées par organisation
  - [x] 2.2 Backend : pièces d'une affaire (nom, surface) et lots de l'organisation (liste par défaut créée avec l'organisation)
  - [x] 2.3 Backend : lignes de devis (pièce × lot, désignation, unité, quantité, PU HT, TVA) + synthèses par pièce et par lot (HT, TVA, TTC)
  - [x] 2.4 Backend : bibliothèque d'ouvrages de l'organisation, ajout d'une ligne depuis la bibliothèque
  - [ ] 2.5 Frontend : liste et création des affaires
  - [ ] 2.6 Frontend : écran affaire avec pièces, lignes de devis, bascule vue par pièce / vue par lot, totaux
  - [ ] 2.7 Test de bout en bout + démo à montrer à la chargée d'études
- [ ] Plus tard (dépend de l'entretien métier) : CR de visite avec photos, estimation en fourchette, export / coexistence ProGBat, passage au chantier, travaux supplémentaires, marge réelle

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

- Vocabulaire métier en français dans le code du domaine (`Affaire`, `Piece`, `Lot`, `LigneDevis`), pour coller au langage de la chargée d'études.
- Montants en `BigDecimal` / `NUMERIC` (quantités 3 décimales, prix et montants 2 décimales), TVA par ligne (5,5 / 10 / 20 %). Les totaux sont calculés côté serveur.
- Multi-entreprise dès le départ : chaque donnée métier porte `organisation_id` ; un utilisateur appartient à une organisation (créée à sa première connexion en attendant les invitations). Accès à une donnée d'une autre organisation → 404.
- API REST en français : `/api/affaires`, erreurs au format ProblemDetail. Statuts d'affaire : VISITE, ESTIMATION, DOSSIER_TRAVAUX, CHANTIER, TERMINEE, PERDUE.
- Tests d'intégration : `DatabaseCleaner` vide les tables avant chaque test, `TestTokens.user(sub)` simule un utilisateur (un sub = une organisation). Ajouter chaque nouvelle table dans `DatabaseCleaner`.
- Le frontend doit appeler `/api/me` avant les autres appels à la première connexion (création utilisateur + organisation, évite une double création concurrente).
- Contrôleurs : paramètre `@CurrentOrganisation Organisation` (résolu par `CurrentOrganisationResolver`) pour cloisonner les données.
- Les 14 lots par défaut (`LotService.LOTS_PAR_DEFAUT`) sont créés via `OrganisationCreatedEvent` à la création d'une organisation. API `/api/lots` ; code de lot unique par organisation (409 sinon).
- Pièces : `/api/affaires/{id}/pieces`, supprimées en cascade avec l'affaire (`ON DELETE CASCADE`).
- Devis : `GET /api/affaires/{id}/devis` renvoie lignes + totaux par pièce (« Général » pour les lignes sans pièce, toutes les pièces même à 0), par lot (lots utilisés), par taux de TVA, HT/TVA/TTC. Lignes : `POST/PUT/DELETE /api/affaires/{id}/lignes`.
- Calculs (`Montants`) : montant HT ligne = quantité × PU arrondi au centime (demi supérieur) ; TVA calculée par taux sur la base HT cumulée ; taux autorisés 0 / 5,5 / 10 / 20, 10 % par défaut. PU négatif autorisé (remises).
- Supprimer une pièce ou un lot utilisé par des lignes → 409 (`ApiExceptionHandler`).
- Bibliothèque : `/api/ouvrages?q=&lotId=` (recherche par mot dans la désignation). `POST /api/affaires/{id}/lignes/depuis-ouvrage {ouvrageId, pieceId, quantite}` copie l'ouvrage dans la ligne et garde `ouvrage_id` (lien perdu si l'ouvrage est supprimé, `ON DELETE SET NULL`) : base de la future boucle prix de référence / prix devisé / coût réel.
- `Montants` est dans `com.faro.common` ; `Montants.tauxTva(saisi)` valide et normalise le taux (400 si invalide).
- Une ligne de devis a toujours un lot ; la pièce est facultative (ligne « générale » : installation de chantier, évacuation…).

## Bloquants / questions

- Questions de cadrage du MVP : voir `docs/brainstorm.md`.
- Un fournisseur OIDC est-il imposé en production (Google, Entra, Auth0…) ? Keycloak par défaut en attendant.
