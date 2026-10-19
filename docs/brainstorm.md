# Brainstorm produit faro

> Source : `docs/business-plan-v1.0.pdf` (business plan v1.0 du 02/03/2026).
> Ce fichier garde la trace des échanges de cadrage avec l'utilisateur, pour qu'une nouvelle session puisse reprendre.

## Résumé du business plan

**faro = cockpit SaaS de pilotage de projets de rénovation (surtout résidentielle) et de construction.**
Remplacer Excel / mails / WhatsApp par un fil numérique continu :
périmètre (CCTP/DPGF) → budget → achats → planning → exécution → réception.

**Différenciation clé :** un *moteur d'impact* (coût / délai / risque) déclenché à chaque décision
(choix matériau, changement de plan, avenant, retard fournisseur) + traçabilité opposable
(versioning, validations, preuves photo) + pilotage par zones (pièces/étages).

**Profils cibles :**
- B — Économiste / MOE : DCE, budget, analyse d'offres, avenants, tableaux de bord.
- Architecte / architecte d'intérieur : sélections de finitions reliées au budget, références fournisseurs, commandes.
- C — Client particulier : espace simple, arbitrages 3 options (Eco/Standard/Premium), validations, photos.
- D — Investisseur / marchand de biens : ROI/TRI, cash-flow, portefeuille multi-opérations.

**Noyau commun :** projet + découpage (lots → ouvrages → tâches, et zones), documents & versioning,
budget dynamique (prévu / engagé / payé / reste-à-engager, aléas, EAC), planning (Gantt, dépendances,
chemin critique, zones), workflow de changements/avenants, journal de chantier & preuves, réserves, CR, tableaux de bord.

**Roadmap du BP :** MVP 6–12 mois = noyau + module Client + module Investisseur v1 + connecteurs essentiels.
V2 = achats/supply chain, analyse d'offres, IA métier, API.

**Modèle économique :** SaaS par utilisateur (Starter/Pro/Business) + modules activables, ou forfait par projet ;
services, data/reporting, white label.

**Concurrents cités :** Réa, ProGBat, AGLO, Koutravo.

## Premières observations (Claude)

1. Le MVP du BP est très large pour 6–12 mois : il faut un « coin d'entrée » plus étroit et démontrable.
2. Le cœur différenciant est le modèle de données relié (ouvrage × zone × ligne budgétaire × décision) :
   c'est lui qu'il faut bien concevoir en premier, le reste s'y branche.
3. Le moteur d'impact n'a de valeur que si budget **et** planning sont reliés aux mêmes objets.
4. Multi-organisation (agence MOE avec ses clients invités) à prévoir dès le modèle de données.

## Contexte (réponses de l'utilisateur)

- **Cible commerciale : les entreprises de rénovation** (pas d'abord les MOE / investisseurs du BP).
- L'idée vient de la femme de l'utilisateur, **chargée d'études** dans le métier → experte métier et première utilisatrice pilote potentielle.
- L'utilisateur est développeur ; objectif : vendre le SaaS.

## Entretien métier — chargée d'études (2026-10-19)

1. **Déroulé d'une affaire**
   - Visite du projet avec le commercial : prise de notes et de photos.
   - Rédaction d'un CR de visite détaillé (la plupart du temps).
   - Estimation « à la grosse » avec un outil peu détaillé → **fausse dans ~90 % des cas**.
   - Si le client est d'accord : **dossier travaux (DT)** = plans + devis détaillé.
   - Le DT est transmis aux **conducteurs de travaux** pour la réalisation.
2. **Outils actuels** : **ProGBat** pour les devis ; un autre logiciel (non identifié) pour la compta, les CR, etc.
3. **Devis** : structuré **par pièces** (jugé absurde) ; bibliothèque de prix construite **sans vraies données** ;
   **~1 jour** pour faire un devis.

Questions 4 à 8 (pertes d'argent, modifications en chantier, marge, taille de l'entreprise, baguette magique) : pas encore répondues.

### Irritants identifiés

- L'estimation initiale est presque toujours fausse → attentes client faussées, marge ou affaires perdues.
- Les prix de la bibliothèque ne reposent pas sur des coûts réels, et rien ne remonte du chantier vers la bibliothèque.
- Un devis prend une journée.
- Structure unique par pièces, alors que l'exécution (conducteur, artisans, achats) raisonne par lots / corps d'état.
- Passage DT → conducteur de travaux : risque de perte d'information (à confirmer).

## Conséquences (Claude)

- Le persona principal devient l'entreprise de rénovation, avec ses rôles internes :
  chargé d'études (métré, chiffrage, devis), conducteur de travaux (exécution), gérant (marge, trésorerie),
  et le client final invité (choix, validations).
- Attention : sur le devis/facturation pur, on rejoint les ERP BTP (Réa, ProGBat, Obat, Tolteck…).
  La différenciation doit rester : zones + options Eco/Standard/Premium + travaux supplémentaires chiffrés et validés
  + suivi de marge prévue vs réelle, du devis jusqu'à la réception.
- Piste de MVP : demande client → métré par zones → devis structuré lots/ouvrages/zones (bibliothèque de prix)
  → options → validation client → passage au chantier (budget) → travaux supplémentaires → marge réelle.

## Pistes issues de l'entretien (Claude)

- **Devis matriciel pièces × lots** : on saisit chaque ouvrage une seule fois, rattaché à une pièce et à un lot ;
  vue par pièce pour le client, vue par lot pour le conducteur / les artisans / les achats.
  C'est exactement le modèle « ouvrage × zone » du business plan.
- **Visite → CR → estimation dans le même outil** : le CR de visite (notes, photos, pièces, métrés) alimente directement
  l'estimation puis le DT, sans ressaisie.
- **Estimation fiable** : ratios et prix alimentés par les coûts réels des chantiers terminés (boucle de retour),
  avec une fourchette affichée plutôt qu'un chiffre unique.
- **Coexistence avec ProGBat** : à clarifier (remplacer la partie devis, ou compléter / exporter vers ProGBat).

## Questions ouvertes

- Entretien métier avec la chargée d'études : voir la liste dans la conversation du 2026-10-17 (outils actuels,
  déroulé d'une affaire, irritants, ce qui ferait acheter le patron).
- Taille des entreprises visées, nombre d'utilisateurs par entreprise.
- Logiciel compta / CR de l'entreprise (non identifié).
- Pourquoi le devis par pièces est absurde : quelle structure préférée (lots / corps d'état) ?
- Pourquoi l'estimation est fausse : postes oubliés, ratios faux, découvertes en chantier ?
- L'entreprise veut-elle remplacer ProGBat ou garder ProGBat à côté ?
- Questions 4 à 8 de l'entretien.

## Décisions

_Aucune pour l'instant._
