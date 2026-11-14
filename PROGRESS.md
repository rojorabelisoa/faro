# Avancement du projet faro

> Fichier de suivi lu au début de chaque session Claude (voir `CLAUDE.md`).
> Dernière mise à jour : 2026-11-14

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
- FAIT (2026-11-12) : tableau de bord « verre dépoli » en page d'accueil (`/`, inspiré de la vidéo « Glassy Dashboard V24 » envoyée par l'utilisateur) : `GET /api/tableau-de-bord` (`com.faro.tableaubord`) → affaires actives, signé ce mois (dossiers travaux + avenants validés) avec tendance, marge réelle, en attente client, taux de transformation, signé par mois sur 12 mois, montant par étape, liste « À faire » (avenants en attente, affaires à relancer après 14 jours, client ayant consulté son espace). Frontend `pages/TableauDeBordPage.tsx` + `components/tableau/*` + `styles/tableau-de-bord.css` (animations `motion`, couleurs validées par le script dataviz : corail #d9482b clair / #e8674a sombre). La liste des affaires est sur `/affaires`.
- FAIT (2026-11-12) : style « verre dépoli » étendu à toute l'application : barre latérale avec icônes à la place du menu du haut (tiroir sur mobile), fond pêche / corail global, cartes et tableaux translucides, accent corail, espace client et page de connexion au même style, étiquettes de mois lisibles sur mobile. Fichiers : `frontend/src/App.tsx`, `styles.css`, `styles/tableau-de-bord.css`, `components/Icones.tsx`.
- FAIT (2026-11-13) : présentation commerciale PowerPoint pour un commercial (`docs/faro-presentation-commerciale.pptx`, 13 diapos avec notes de l'orateur : problème, déroulé de l'affaire, captures des données de démo, cible, comparatif, offres avec prix à définir, objections, prochaines étapes). Script de génération pptxgenjs : `docs/presentation/build.js` (images à recapturer dans `img/` à côté du script).
- Lot sécurité du 2026-11-14 (3 étapes, une par commit) :
  - [x] 1. Droits par rôle dans l'entreprise (FAIT) : `com.faro.droits` (`Droits`, `@AccesConducteur`, `DroitsInterceptor`), endpoint `GET /api/affaires/{id}/depenses`, tableau de bord réduit (`montants: false`), frontend `src/droits.ts` (boutons, sections et menus masqués). Tests `DroitsApiTest`, `AffairePage.test`, `TableauDeBordPage.test`.
  - [x] 2. Espace client (FAIT) : expiration des liens (migration V17 `acces_client.expire_at`, `faro.client.duree-lien-jours` = 90, `POST /api/affaires/{id}/acces-client/{lienId}/prolonger`, date affichée dans `AccesClient.tsx`) et limite de 60 requêtes / minute / IP sur `/api/public/**` (`LimiteRequetesPubliques`, 429 + `Retry-After`). Corrigé au passage : le déboursé sec n'est plus envoyé à l'espace client. Tests `EspaceClientApiTest`, `LimiteRequetesPubliquesTest`, `AccesClient.test.tsx`.
  - [ ] 3. Photos de visite : vérification du contenu réel (signature, décodage ImageIO, dimensions), type enregistré = type détecté
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
- Espace client : `/api/public/**` est public (SecurityConfig), protégé par le code secret du lien. Depuis le 2026-11-14 :
  - les liens expirent (`expire_at`, 90 jours par défaut, `faro.client.duree-lien-jours` / `FARO_CLIENT_DUREE_LIEN_JOURS` ; liens existants : 90 jours à partir de la migration V17). Un lien expiré répond exactement comme un lien inconnu ou révoqué (404 « Lien invalide ou révoqué ») sur toutes les routes publiques. L'entreprise voit toujours ses liens expirés (non révoqués) et peut les prolonger (90 jours à partir du jour de la prolongation) ; un lien révoqué ne se prolonge pas (404). Prolonger est refusé au conducteur de travaux (comme tout ce qui touche aux liens).
  - limite en mémoire de 60 requêtes / minute / adresse IP sur `/api/public/**` (fenêtre fixe, filtre servlet avant Spring Security, 429 ProblemDetail + `Retry-After`), `faro.public.limite.active` / `requetes-par-minute` / `proxys-de-confiance` (`FARO_LIMITE_PUBLIC_ACTIVE`, `FARO_LIMITE_PUBLIC_PAR_MINUTE`, `FARO_PROXYS_DE_CONFIANCE`). Désactivée dans les tests (`src/test/resources/application.properties`), testée à part avec un contexte dédié.
  - adresse du client : `server.forward-headers-strategy` n'est pas configuré (rien ne lisait X-Forwarded-For). Choix prudent : X-Forwarded-For n'est lu que si la connexion vient d'un réseau privé / boucle locale et que `proxys-de-confiance` > 0 ; on prend alors la N-ième entrée en partant de la droite (celle ajoutée par le proxy le plus extérieur), ce que le client écrit lui-même est ignoré. Chaînes : production et réseau local = Caddy (qui remplace l'en-tête d'un client non fiable) + nginx (`$proxy_add_x_forwarded_for`) → 2 ; Docker « complet » = nginx → 1 ; backend direct (dév.) → 0 (défaut). Réglé dans `docker-compose.yml`, `docker-compose.reseau.yml`, `deploy/docker-compose.prod.yml` ; documenté dans `docs/deploiement.md` (section 5). Compteur propre à chaque instance (à revoir si plusieurs backends).
- Lancement complet : `docker compose --profile complet up -d --build` → http://localhost:3000 (backend et frontend dans Docker ; le backend lit les clés Keycloak sur le réseau Docker et vérifie l'émetteur `http://localhost:8180/realms/faro`). Comptes de démo avec identifiants fixes dans le realm (`00000000-0000-4000-8000-00000000000x`) et volume `keycloak-data` : recréer Keycloak ne détache plus les utilisateurs de leurs données.
- Données de démo : `com.faro.demo.DonneesDemo` (propriété `faro.demo.donnees`, `FARO_DEMO_DONNEES=true` dans le profil Docker complet) charge au démarrage, seulement si aucune affaire n'existe, l'entreprise « Rénov Lyon (démo) » rattachée aux comptes demo (gérant) et admin (chargé d'études), 21 ouvrages, 10 photos de visite dessinées (`PhotosDemo`), 7 affaires (T3 signé le jour du chargement, maison en chantier, studio terminé avec bilan investisseur, appartement avec gammes, loft en estimation, extension en visite, affaire perdue).
- Mode réseau local (wifi) : `docker-compose.reseau.yml` + `docker/Caddyfile.reseau` + realm `keycloak/reseau/` (redirections `${FARO_URL}`) : tout sur `https://FARO_HOTE` (Caddy `tls internal`, Keycloak sous `/auth`, autorité OIDC déduite de l'adresse de la page quand `VITE_OIDC_AUTHORITY` est vide). Testé (contexte sécurisé, connexion, tableau de bord). Doc : `docs/reseau-local.md` (WSL `networkingMode=mirrored` + pare-feu). PC de l'utilisateur : 192.168.1.84.
- Volume Keycloak monté sur `/opt/keycloak/data` (le sous-dossier `h2` n'existe pas dans l'image : volume créé en root, Keycloak ne pouvait pas y écrire).
- Thème : jetons dans `styles.css` (`--surface` translucide, `--bord-verre`, `--flou`, `--fond-degrade` sur `html`, accent corail `--accent` #c63f24 clair / #e8674a sombre avec `--accent-texte`, liens `--accent-lien`). Contrastes vérifiés (texte ≥ 4,5:1). Mise en page : `.app` = barre latérale + `.principal` ; en dessous de 900 px la barre devient un tiroir (`.menu-ouvert`).
- Droits par rôle d'entreprise (2026-11-14) — matrice par défaut, À VALIDER par l'utilisateur :
  - GERANT : tout. CHARGE_ETUDES : tout sauf l'administration de l'entreprise (déjà réservée au gérant par `EquipeService`).
  - CONDUCTEUR_TRAVAUX : lecture des affaires, pièces, lots, devis (écran, PDF client / chantier, CSV), versions figées (et leurs PDF), bons de commande, visite (et PDF), avenants et montant du marché ; peut ajouter des notes et des photos de visite (pas les supprimer), créer / modifier un avenant, y ajouter / modifier / supprimer des lignes tant qu'il est en brouillon, saisir / modifier / supprimer les dépenses réelles (`GET /api/affaires/{id}/depenses` lui donne la liste sans la marge). Refusé (403 « Action non autorisée pour un conducteur de travaux… ») : créer / modifier / supprimer affaire (statut compris), pièces, lignes du devis de base, gammes, versions, passage en chantier, bibliothèque (lecture et export compris), import CSV, lots, ratios, estimation, liens d'espace client (lecture comprise : le lien est un secret), marge, bilan investisseur. Choix prudents : il ne peut ni supprimer un avenant, ni l'envoyer au client, ni enregistrer la décision du client ; il ne voit pas le déboursé sec (masqué dans le devis, les versions et les avenants ; s'il modifie une ligne d'avenant, le déboursé existant est conservé).
  - Tableau de bord du conducteur : `montants: false`, seuls restent le nombre d'affaires actives, le nombre d'affaires par étape et la liste « À faire » ; indicateurs financiers et taux de transformation à null, signé par mois vide.
  - Mécanisme : refus par défaut. `DroitsInterceptor` (sur `/api/**` sauf `/api/public/**`, `/api/admin/**`, `/api/me`, `/api/health`) refuse au conducteur tout endpoint qui n'est pas annoté `@AccesConducteur` (méthode ou contrôleur) ; un nouvel endpoint lui est donc fermé tant qu'on ne l'ouvre pas explicitement. Les règles fines (ligne d'avenant ou de base, déboursé masqué, tableau réduit) passent par le composant `Droits` (`estConducteur()`, `voitLesMontantsInternes()`, `refuserConducteur()`), qui charge l'utilisateur une fois par requête (partagé avec `@CurrentOrganisation`). Frontend : `src/droits.ts` (`useDroits()` → `gestion`, `montantsInternes`) masque boutons, sections et menus (Bibliothèque, Lots, Ratios) ; le serveur fait foi.
- Une ligne de devis a toujours un lot ; la pièce est facultative (ligne « générale » : installation de chantier, évacuation…).

## Bloquants / questions

- Questions de cadrage du MVP : voir `docs/brainstorm.md`.
- ProGBat : obtenir un exemple de fichier d'import / export ProGBat (devis et bibliothèque) pour adapter les formats CSV.
- IA : choisir les premiers cas d'usage et fournir une clé API.
- Ratios d'estimation : faire valider / remplacer les valeurs indicatives par la chargée d'études.
- Offres : fixer les prix et les limites (Starter / Pro / Business), choisir le prestataire de paiement (Stripe…) et ce qui se passe à la fin de l'essai.
- Hébergement : choisir un serveur (VPS) et un nom de domaine pour la mise en ligne (voir `docs/deploiement.md`).
- Droits par rôle : valider la matrice par défaut (voir « Décisions », 2026-11-14), en particulier pour le conducteur de travaux : envoi des avenants au client et saisie de la décision, suppression d'un avenant en brouillon, suppression / modification de ses notes et photos de visite, accès en lecture à la bibliothèque et à l'estimation, visibilité du taux de transformation.
- Un fournisseur OIDC est-il imposé en production (Google, Entra, Auth0…) ? Keycloak par défaut en attendant.
