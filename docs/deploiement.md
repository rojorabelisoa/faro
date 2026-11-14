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

## 5. Espace client : liens et limite de requêtes

- Les liens d'espace client expirent au bout de 90 jours (`FARO_CLIENT_DUREE_LIEN_JOURS`) ; l'entreprise peut
  prolonger un lien depuis la fiche de l'affaire (90 jours de plus à partir du jour de la prolongation). Un lien
  expiré répond comme un lien inconnu.
- Les requêtes sur `/api/public/**` (espace client, sans compte) sont limitées à 60 par minute et par adresse IP
  (`FARO_LIMITE_PUBLIC_PAR_MINUTE`, désactivable avec `FARO_LIMITE_PUBLIC_ACTIVE=false`) ; au-delà, réponse 429
  avec `Retry-After`. Le compteur est en mémoire : propre à chaque instance du backend, remis à zéro au redémarrage.
- Adresse du client : le backend ne voit que l'adresse du proxy. `FARO_PROXYS_DE_CONFIANCE` indique combien de
  proxys faro sont devant lui (ici Caddy puis nginx : `2`, déjà réglé dans `docker-compose.prod.yml`). L'adresse
  retenue est alors l'entrée de `X-Forwarded-For` ajoutée par le proxy le plus extérieur (la 2e en partant de la
  droite), et seulement si la connexion vient d'un réseau privé ; ce que le client met lui-même dans l'en-tête est
  ignoré. Si vous ajoutez ou retirez un proxy (CDN, répartiteur de charge…), ajustez cette valeur, sinon tous les
  clients partagent la même limite (valeur trop basse) ou un client peut choisir son adresse (valeur trop haute).

## 6. Ce qui a été vérifié, et ce qui reste à faire

Vérifié (session de développement, sans domaine ni HTTPS) : la stack de production démarre
(Keycloak en mode `start` sur PostgreSQL, backend, frontend nginx), l'inscription d'un nouvel utilisateur,
la création d'une affaire, le proxy `/api`, le rechargement des pages de l'application, la validation des jetons
(clés lues en interne, émetteur public vérifié).

Non vérifié ici : Caddy avec un vrai domaine (HTTPS). La construction des images Docker multi-étapes
(`backend/Dockerfile`, `frontend/Dockerfile`) est vérifiée par la CI GitHub (job `docker`).

Avant d'ouvrir à des clients :
- vérifier avec le vrai domaine que la limite de requêtes de l'espace client voit bien l'adresse des clients
  (section 5) ;
- configurer l'envoi d'e-mails dans Keycloak ;
- surveiller l'espace disque (photos) et tester une restauration de sauvegarde ;
- mentions légales, conditions d'utilisation et politique de confidentialité (RGPD).
