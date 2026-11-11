# Mettre faro en ligne

Tout tient sur **un seul serveur** avec Docker : PostgreSQL, Keycloak (comptes et connexion),
le backend Spring Boot, le frontend (nginx) et Caddy, qui obtient et renouvelle les certificats HTTPS
(Let's Encrypt) tout seul.

```
Internet ──► Caddy :443 ──► faro.example.fr ──► frontend (nginx) ──► /api ──► backend ──► PostgreSQL
                       └──► auth.faro.example.fr ──► Keycloak ─────────────────────────┘
```

## 1. Ce qu'il faut

- Un serveur Linux (VPS) avec 2 vCPU et 4 Go de RAM au minimum (OVH, Scaleway, Hetzner… : ~10 à 20 €/mois),
  Docker et Docker Compose installés.
- Un nom de domaine, avec deux enregistrements DNS **A** vers l'adresse IP du serveur :
  `faro.example.fr` (l'application) et `auth.faro.example.fr` (la connexion).
- Les ports 80 et 443 ouverts.

## 2. Installer

```bash
git clone https://github.com/rojorabelisoa/faro.git
cd faro/deploy
cp .env.example .env
nano .env        # domaines et mots de passe (longs et différents)
docker compose -f docker-compose.prod.yml up -d --build
```

Au premier démarrage :
- PostgreSQL crée les bases `faro` et `keycloak` (`postgres-init.sh`) ;
- Keycloak importe le realm `faro` (`keycloak/faro-realm.json`) : inscription ouverte, aucun compte de démo,
  adresses de retour sur `https://FARO_DOMAINE` ;
- Caddy obtient les certificats (il faut que le DNS pointe déjà vers le serveur).

Ouvrir `https://faro.example.fr`, cliquer sur « Se connecter » puis « S'enregistrer » : le premier compte crée
son entreprise et en devient le gérant ; il invite ensuite ses collègues depuis la page **Entreprise**.

Console d'administration Keycloak : `https://auth.faro.example.fr` (identifiants `KEYCLOAK_ADMIN` du `.env`).
À régler dans la console avant d'ouvrir au public : envoi d'e-mails (SMTP, pour « mot de passe oublié »),
vérification des adresses e-mail, politique de mots de passe.

## 3. Mettre à jour

```bash
cd faro && git pull
cd deploy && docker compose -f docker-compose.prod.yml up -d --build
```

Les migrations de base de données (Flyway) s'appliquent toutes seules au démarrage du backend.

## 4. Sauvegarder (indispensable)

Les données (affaires, devis, photos) sont dans PostgreSQL. Sauvegarde quotidienne, à mettre dans la crontab
du serveur et à copier hors du serveur (stockage objet, autre machine) :

```bash
docker compose -f /chemin/faro/deploy/docker-compose.prod.yml exec -T postgres \
  pg_dumpall -U faro | gzip > /sauvegardes/faro-$(date +%F).sql.gz
```

Restauration : `gunzip -c faro-AAAA-MM-JJ.sql.gz | docker compose -f docker-compose.prod.yml exec -T postgres psql -U faro`.

## 5. Ce qui a été vérifié, et ce qui reste à faire

Vérifié (session de développement, sans domaine ni HTTPS) : la stack de production démarre
(Keycloak en mode `start` sur PostgreSQL, backend, frontend nginx), l'inscription d'un nouvel utilisateur,
la création d'une affaire, le proxy `/api`, le rechargement des pages de l'application, la validation des jetons
(clés lues en interne, émetteur public vérifié).

Non vérifié ici : Caddy avec un vrai domaine (HTTPS), et la construction des images Docker multi-étapes
(`backend/Dockerfile`, `frontend/Dockerfile`) qui demande l'accès à Maven Central et npm depuis Docker.

Avant d'ouvrir à des clients :
- limiter le débit des requêtes publiques (`/api/public/*` de l'espace client), par exemple dans Caddy ;
- configurer l'envoi d'e-mails dans Keycloak ;
- surveiller l'espace disque (photos) et tester une restauration de sauvegarde ;
- mentions légales, conditions d'utilisation et politique de confidentialité (RGPD).
