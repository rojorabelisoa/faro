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

- Phase 3 terminée pour tout ce que Claude pouvait faire (points 4 à 23). Restent : points 1 à 3 (utilisateur) et les parties bloquées listées dans « Bloquants / questions ».
- Prochaine étape : démo à la chargée d'études avec toutes les fonctions, puis prioriser ses retours.
- EN COURS (2026-11-12) : tableau de bord « glassy » demandé par l'utilisateur (inspiré d'une vidéo TikTok « Glassy Dashboard V24 » : cartes en verre dépoli corail / pêche, accueil, anneau de progression, cartes chiffres avec mini-courbes, graphiques). Backend `com.faro.tableaubord` (`GET /api/tableau-de-bord`), frontend `pages/TableauDeBordPage.tsx` en page d'accueil (`/`), la liste des affaires passe sur `/affaires`.
- Reprise automatique : la tâche planifiée « Reprise backlog faro » (`trig_01Dtkand7AoEABUZATjsPNrw`) a été désactivée à la fin du backlog (2026-11-11).
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
  - [x] 2.5 Frontend : liste et création des affaires
  - [x] 2.6 Frontend : écran affaire avec pièces, lignes de devis, bascule vue par pièce / vue par lot, totaux
  - [x] 2.7 Test de bout en bout (Playwright : affaire, 3 pièces, 12 lignes, bibliothèque, bascule pièce/lot, totaux vérifiés à la main)
  - [ ] 2.8 Démo à la chargée d'études et recueil de ses retours (dans `docs/brainstorm.md`)
- [ ] Phase 3 : backlog (validé par l'utilisateur le 2026-11-11, « fais jusqu'au 23 »). Une étape = un commit.
  Les points 1 à 3 sont pour l'utilisateur (Claude ne peut pas les faire) ; les points marqués « bloqué » sont faits partiellement, le reste est noté.
  - [ ] 1. Installer faro sur le PC de l'utilisateur (Windows + Docker sous WSL) — utilisateur, via sa session Remote Control
  - [ ] 2. Démo à la chargée d'études, retours dans `docs/brainstorm.md` — utilisateur
  - [ ] 3. Finir l'entretien métier (questions ouvertes de `docs/brainstorm.md`) — utilisateur
  - [x] 4. Export PDF du devis (OpenPDF 3, `GET /api/affaires/{id}/devis.pdf?vue=PIECE|LOT`, boutons « PDF client » / « PDF chantier ») — à compléter plus tard avec les mentions légales de l'entreprise (SIRET, assurance décennale…)
  - [x] 5. Descriptifs détaillés sur les lignes (saisie + affichage) + réordonner les lignes (↑ ↓, `PUT /api/affaires/{id}/lignes/ordre`)
  - [x] 6. Métrés : pièce avec surface, périmètre, hauteur sous plafond (+ calcul longueur × largeur) ; murs = périmètre × HSP (ouvertures non déduites) ; boutons Sol / Murs / Plafond / Périmètre qui remplissent quantité et unité
  - [x] 7. Écrans manquants : modifier/supprimer une affaire (+ e-mail, téléphone, notes), modifier une pièce (clic sur la pastille), page `/lots`, modifier un ouvrage (+ descriptif)
  - [x] 8. Import / export CSV de la bibliothèque (`POST /api/ouvrages/import`, `GET /api/ouvrages/export.csv`) : CSV Excel FR (`;`, UTF-8 ou Windows-1252, « 72,40 € »), en-têtes souples, tout-ou-rien avec erreurs par ligne, réimport = mise à jour des prix (même lot + désignation). Format ProGBat : à confirmer avec un exemple
  - [x] 9. CR de visite : observations et photos par pièce (migration V7, photos en base `bytea`, réduites à 1600 px JPEG par le navigateur, `capture` caméra sur mobile), PDF du compte-rendu (`GET /api/affaires/{id}/visite.pdf`) avec photos et métrés
  - [x] 10. Estimation rapide en fourchette : type de pièce + niveau de travaux sur la pièce (type deviné depuis le nom), ratios €/m² min/max par organisation (migration V8, 24 ratios INDICATIFS à faire valider par la chargée d'études), page `/ratios` avec €/m² constatés dans les devis (médiane), comparaison estimation / devis détaillé
  - [x] 11. Options Eco / Standard / Premium : gamme facultative par ligne (null = commune), gamme choisie par pièce et par affaire pour les lignes générales (migration V9, `PUT .../pieces/{id}/gamme`, `PUT /api/affaires/{id}/gamme`), totaux / TVA / PDF / vue par lot sur les lignes retenues, prix de chaque gamme par pièce et comparatif global
  - [x] 12. Versions figées du devis (migration V10, JSON complet du devis + en-tête dans `devis_version.contenu`, numérotées, PDF par version) et passage en chantier (`POST /api/affaires/{id}/chantier` : fige « Dossier travaux », statut CHANTIER, `affaire.version_chantier_id` = référence pour avenants et marge)
  - [x] 13. Avenants (migration V11) : lignes de devis rattachées à un avenant (`ligne_devis.avenant_id`), cycle BROUILLON → ENVOYE (contenu figé en JSON) → VALIDE / REFUSE (qui, quand, commentaire), PDF, montant du marché (`GET /api/affaires/{id}/marche` = dossier travaux + avenants validés) ; devis de base verrouillé (409) une fois en chantier
  - [x] 14. Marge (migration V12) : déboursé sec unitaire facultatif sur lignes et ouvrages (copié depuis la bibliothèque, colonne CSV `debourse_unitaire_ht`), dépenses réelles par lot (`/api/affaires/{id}/depenses`), `GET /api/affaires/{id}/marge` : vendu (lignes retenues + avenants validés), coût prévu / réel, marges et taux par lot et au total, lignes sans déboursé signalées
  - [x] 15. Export CSV « Excel » du devis (`GET /api/affaires/{id}/devis.csv`, lignes retenues, lot / pièce / désignation / quantité / PU / TVA / montant / gamme). Format d'import ProGBat : BLOQUÉ, à adapter dès qu'on a un fichier exemple
  - [x] 16. Équipe (migration V13) : rôle d'entreprise (GERANT, CHARGE_ETUDES, CONDUCTEUR_TRAVAUX) sur l'utilisateur, invitations par lien à usage unique valable 7 jours (`/api/entreprise/invitations`, `/api/invitations/{code}/accepter`), renommage de l'entreprise, changement de rôle / retrait d'un membre (gérant seulement, au moins un gérant), page `/entreprise` et `/rejoindre/{code}` ; inscription activée dans le realm Keycloak
  - [x] 17. Espace client (migration V14) : lien secret révocable par affaire (`/api/affaires/{id}/acces-client`, code aléatoire 32 octets), page publique `/client/{code}` sans compte (`/api/public/client/{code}` en `permitAll`) : devis par pièce (lignes retenues), choix des gammes avant chantier, PDF, avenants envoyés avec acceptation / refus (« Nom (espace client) »), montant du marché ; dernière visite affichée à l'équipe
  - [x] 18. Déploiement préparé : `backend/Dockerfile`, `frontend/Dockerfile` + `nginx.conf` (proxy /api, SPA), `deploy/docker-compose.prod.yml` (PostgreSQL + Keycloak `start` + backend + frontend + Caddy HTTPS), realm de production sans comptes de démo (`deploy/keycloak`), `docs/deploiement.md`. Stack de production testée en local (inscription, affaire). Mise en ligne réelle BLOQUÉE : choisir hébergeur et nom de domaine ; images multi-étapes et Caddy/HTTPS non testés ici
  - [x] 19. Offres (migration V15) : plan par entreprise ESSAI (30 jours, 3 utilisateurs) / STARTER (1) / PRO (10) / BUSINESS (illimité) — limites INDICATIVES —, limite d'utilisateurs appliquée aux invitations, bandeau de fin d'essai, page `/admin` (rôle Keycloak `admin`) pour voir les entreprises et changer leur offre. Paiement en ligne BLOQUÉ : compte Stripe (ou autre) et prix à définir ; la fin d'essai ne bloque rien pour l'instant
  - [x] 20. CI sur toutes les branches (push sur toute branche + PR ; backend via `./mvnw`)
  - [x] 21. `docker-compose.yml` testé de bout en bout (image Keycloak prise sur Docker Hub, quay.io étant bloqué dans la session cloud) : connexion + scénario devis Playwright OK
  - [x] 22. Protection serveur contre la double création d'utilisateur à la première connexion (verrou `pg_advisory_xact_lock` par subject, test de concurrence à 8 threads)
  - [x] 23. Plus tard (business plan), version 1 : bilan d'opération investisseur (migration V16 ; achat, frais, travaux = marché actuel TTC, revente, durée → marge, rentabilité, TRI annualisé simplifié, scénarios travaux +10 % et retard +3 mois) et bons de commande PDF par lot (ouvrages retenus + avenants validés, sans prix). IA BLOQUÉE : clé API (ex. Claude) et cas d'usage à valider (lecture de devis PDF, détection d'oublis)

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
- Session cloud : Docker est disponible (`dockerd &` puis `docker compose up -d`, après `service postgresql stop` pour libérer le port 5432 ; les tests backend tournent alors sur le PostgreSQL du compose, mêmes identifiants). Sinon : Keycloak peut être lancé depuis le zip officiel (`bin/kc.sh start-dev --import-realm --http-port=8180`) et PostgreSQL 16 est installé localement (`service postgresql start`).
- Les tests backend sont des tests d'intégration sur un vrai PostgreSQL (base `faro`, utilisateur `faro/faro`), pas de Docker requis pour les tests.

- Vocabulaire métier en français dans le code du domaine (`Affaire`, `Piece`, `Lot`, `LigneDevis`), pour coller au langage de la chargée d'études.
- Montants en `BigDecimal` / `NUMERIC` (quantités 3 décimales, prix et montants 2 décimales), TVA par ligne (5,5 / 10 / 20 %). Les totaux sont calculés côté serveur.
- Multi-entreprise dès le départ : chaque donnée métier porte `organisation_id` ; un utilisateur appartient à une organisation (créée à sa première connexion en attendant les invitations). Accès à une donnée d'une autre organisation → 404.
- API REST en français : `/api/affaires`, erreurs au format ProblemDetail. Statuts d'affaire : VISITE, ESTIMATION, DOSSIER_TRAVAUX, CHANTIER, TERMINEE, PERDUE.
- Tests backend sur une base dédiée `faro_test` (`src/test/resources/application.properties`, variable `FARO_TEST_DB_URL`), créée par `docker/postgres-init.sql` sur un volume neuf ; la base `faro` de développement n'est plus vidée par les tests. Dans la session cloud sans Docker : `su postgres -c "createdb -O faro faro_test"`.
- Tests d'intégration : `DatabaseCleaner` vide les tables avant chaque test, `TestTokens.user(sub)` simule un utilisateur (un sub = une organisation). Ajouter chaque nouvelle table dans `DatabaseCleaner`.
- Première connexion : `AppUserService.findOrCreate` prend un verrou PostgreSQL par subject, la création concurrente est sûre côté serveur (le frontend appelle quand même `/api/me` en premier).
- Contrôleurs : paramètre `@CurrentOrganisation Organisation` (résolu par `CurrentOrganisationResolver`) pour cloisonner les données.
- Les 14 lots par défaut (`LotService.LOTS_PAR_DEFAUT`) sont créés via `OrganisationCreatedEvent` à la création d'une organisation. API `/api/lots` ; code de lot unique par organisation (409 sinon).
- Pièces : `/api/affaires/{id}/pieces`, supprimées en cascade avec l'affaire (`ON DELETE CASCADE`).
- Devis : `GET /api/affaires/{id}/devis` renvoie lignes + totaux par pièce (« Général » pour les lignes sans pièce, toutes les pièces même à 0), par lot (lots utilisés), par taux de TVA, HT/TVA/TTC. Lignes : `POST/PUT/DELETE /api/affaires/{id}/lignes`.
- Calculs (`Montants`) : montant HT ligne = quantité × PU arrondi au centime (demi supérieur) ; TVA calculée par taux sur la base HT cumulée ; taux autorisés 0 / 5,5 / 10 / 20, 10 % par défaut. PU négatif autorisé (remises).
- Supprimer une pièce ou un lot utilisé par des lignes → 409 (`ApiExceptionHandler`).
- Bibliothèque : `/api/ouvrages?q=&lotId=` (recherche par mot dans la désignation). `POST /api/affaires/{id}/lignes/depuis-ouvrage {ouvrageId, pieceId, quantite}` copie l'ouvrage dans la ligne et garde `ouvrage_id` (lien perdu si l'ouvrage est supprimé, `ON DELETE SET NULL`) : base de la future boucle prix de référence / prix devisé / coût réel.
- `Montants` est dans `com.faro.common` ; `Montants.tauxTva(saisi)` valide et normalise le taux (400 si invalide).
- Frontend : React Router 7 (`/`, `/affaires/:id`, `/bibliotheque`), client API typé `createApi(token)` dans `src/api.ts`, session (api + me) via `SessionContext`. Saisie des nombres à la française (« 4,25 »). Le regroupement par pièce / par lot se fait côté front à partir de `GET /devis`.
- Ordre des lignes : choisi par l'utilisateur (champ `ordre`), utilisé tel quel dans les deux vues (pièce et lot) ; plus de tri automatique par lot dans la vue par pièce.
- Gammes : une ligne est « incluse » si elle est commune ou de la gamme retenue de sa pièce (ou de l'affaire si générale). Tous les totaux, la TVA, le PDF, la vue par lot et les €/m² constatés ne comptent que les lignes incluses.
- JSON : Spring Boot 4 fournit Jackson 3 (`tools.jackson.databind.json.JsonMapper`), pas `com.fasterxml.jackson.databind`.
- Devis de base = lignes sans avenant (`findByAffaireIdAndAvenantIsNullOrderByOrdreAsc`). En chantier (`version_chantier_id` non nul), création / modification / suppression / réordonnancement des lignes de base → 409 ; les lignes d'avenant ne sont modifiables que si l'avenant est en BROUILLON. `Totaux.de(...)` calcule HT / TVA par taux / TTC pour tout ensemble de lignes.
- Équipe : seul le gérant administre l'entreprise (403 sinon). Accepter une invitation supprime l'entreprise créée à la première connexion si elle est vide. `EquipeService` recharge l'utilisateur dans sa transaction (celui de `AppUserService.current` est détaché).
- Realm Keycloak : `registrationAllowed: true` (inscription). Un realm déjà importé n'est pas mis à jour (import IGNORE_EXISTING) : activer « User registration » dans la console Keycloak ou recréer le volume.
- Espace client : `/api/public/**` est public (SecurityConfig), protégé uniquement par le code secret du lien. Pas encore de limitation de débit (à ajouter avant la mise en production, ex. au niveau du reverse proxy).
- Une ligne de devis a toujours un lot ; la pièce est facultative (ligne « générale » : installation de chantier, évacuation…).

## Bloquants / questions

- Questions de cadrage du MVP : voir `docs/brainstorm.md`.
- ProGBat : obtenir un exemple de fichier d'import / export ProGBat (devis et bibliothèque) pour adapter les formats CSV.
- IA : choisir les premiers cas d'usage et fournir une clé API.
- Ratios d'estimation : faire valider / remplacer les valeurs indicatives par la chargée d'études.
- Offres : fixer les prix et les limites (Starter / Pro / Business), choisir le prestataire de paiement (Stripe…) et ce qui se passe à la fin de l'essai.
- Hébergement : choisir un serveur (VPS) et un nom de domaine pour la mise en ligne (voir `docs/deploiement.md`).
- Un fournisseur OIDC est-il imposé en production (Google, Entra, Auth0…) ? Keycloak par défaut en attendant.
