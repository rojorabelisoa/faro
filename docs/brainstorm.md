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

## Conséquences (Claude)

- Le persona principal devient l'entreprise de rénovation, avec ses rôles internes :
  chargé d'études (métré, chiffrage, devis), conducteur de travaux (exécution), gérant (marge, trésorerie),
  et le client final invité (choix, validations).
- Attention : sur le devis/facturation pur, on rejoint les ERP BTP (Réa, ProGBat, Obat, Tolteck…).
  La différenciation doit rester : zones + options Eco/Standard/Premium + travaux supplémentaires chiffrés et validés
  + suivi de marge prévue vs réelle, du devis jusqu'à la réception.
- Piste de MVP : demande client → métré par zones → devis structuré lots/ouvrages/zones (bibliothèque de prix)
  → options → validation client → passage au chantier (budget) → travaux supplémentaires → marge réelle.

## Questions ouvertes

- Entretien métier avec la chargée d'études : voir la liste dans la conversation du 2026-10-17 (outils actuels,
  déroulé d'une affaire, irritants, ce qui ferait acheter le patron).
- Taille des entreprises visées, nombre d'utilisateurs par entreprise.
- Logiciel actuel de l'entreprise de sa femme (devis, compta).

## Décisions

_Aucune pour l'instant._
