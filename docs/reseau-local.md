# Ouvrir faro aux appareils du wifi

Pour qu'un téléphone, une tablette ou un autre PC du même wifi utilise faro installé sur ton PC.
faro est alors servi en **HTTPS** sur une seule adresse, `https://IP-DU-PC` (la connexion se fait sur `/auth`).
Le HTTPS est obligatoire : les navigateurs refusent la connexion sécurisée (PKCE) sur une adresse en `http://`
autre que `localhost`.

> À réserver à un wifi de confiance (maison, bureau) : toute personne du réseau peut ouvrir la page de connexion.
> Change les mots de passe des comptes de démo (`demo` / `demo`) dans Keycloak (`https://IP-DU-PC/auth`,
> compte `admin` / `admin`), ou crée de vrais comptes.

## 1. Trouver l'adresse du PC

Dans PowerShell : `ipconfig`, ligne « Adresse IPv4 » de la carte **Wi-Fi** (par exemple `192.168.1.84`).
Cette adresse peut changer après un redémarrage de la box : pour la garder, réserve-la dans l'interface de ta box
(« bail DHCP statique » / « réservation d'adresse »).

## 2. Ouvrir le réseau de WSL (une seule fois)

Par défaut, WSL ne rend les ports accessibles qu'au PC lui-même.

1. Crée (ou complète) le fichier `C:\Users\<toi>\.wslconfig` :
   ```ini
   [wsl2]
   networkingMode=mirrored

   [experimental]
   # Sans cette ligne, le PC lui-même ne peut pas ouvrir https://son-IP (les autres appareils, si).
   hostAddressLoopback=true
   ```
2. Dans PowerShell **en administrateur** :
   ```powershell
   # Pare-feu Windows : autoriser HTTPS entrant
   New-NetFirewallRule -DisplayName "faro HTTPS" -Direction Inbound -Protocol TCP -LocalPort 443 -Action Allow
   # Pare-feu Hyper-V de WSL : autoriser les connexions entrantes
   Set-NetFirewallHyperVVMSetting -Name '{40E0AC32-46A5-438A-A0B2-2B479E8F2E90}' -DefaultInboundAction Allow
   wsl --shutdown
   ```
3. Rouvre Ubuntu (et `sudo service docker start` si Docker ne redémarre pas tout seul).

Vérification depuis Windows (PowerShell, avec `curl.exe` et non `curl`) une fois faro lancé :
`curl.exe -k https://192.168.1.84/api/health` doit répondre `{"status":"UP"}`.

Si un appareil n'y arrive pas : le profil réseau Windows doit être « Privé » (`Get-NetConnectionProfile`),
et l'appareil ne doit pas être sur le wifi invité de la box (qui isole les appareils).

Le mode `mirrored` demande Windows 11 (22H2 ou plus récent). Sous Windows 10, remplace l'étape 1 par une redirection
de port, à refaire à chaque redémarrage (adresse de WSL : `hostname -I` dans Ubuntu) :
`netsh interface portproxy add v4tov4 listenport=443 listenaddress=0.0.0.0 connectport=443 connectaddress=<IP de WSL>`.

## 3. Lancer faro en mode réseau

Dans **Ubuntu (WSL)**, pas dans PowerShell, en remplaçant l'adresse par la tienne :

```bash
cd ~/git/faro
docker compose --profile complet down          # arrête le mode localhost s'il tourne
FARO_HOTE=192.168.1.84 docker compose --profile complet -f docker-compose.yml -f docker-compose.reseau.yml up -d --build
```

Les données (affaires, devis…) sont les mêmes qu'en mode localhost : la base PostgreSQL est partagée.

## 4. Sur chaque appareil

Ouvrir `https://192.168.1.84` (avec ton adresse). Le navigateur affiche un avertissement de sécurité, normal ici :
le certificat est fabriqué par ton PC, pas par une autorité publique.

- **Chrome / Edge / Android** : « Paramètres avancés » → « Continuer vers le site ».
- **Safari / iPhone** : « Afficher les détails » → « visiter ce site web ».
- **Firefox** : « Avancé… » → « Accepter le risque et poursuivre ».

C'est à faire une fois par appareil. Ensuite : « Se connecter ».

## Revenir au mode localhost

```bash
FARO_HOTE=192.168.1.84 docker compose --profile complet -f docker-compose.yml -f docker-compose.reseau.yml down
docker compose --profile complet up -d --build
```

## Et pour un accès depuis Internet ?

Le mode réseau local ne suffit pas (box, adresse publique, vrai certificat) : c'est le déploiement décrit dans
[`deploiement.md`](deploiement.md), sur un serveur avec un nom de domaine.
