import { useSession } from './ApiContext'
import type { Me } from './api'

/**
 * Ce que le rôle de l'utilisateur dans l'entreprise lui permet (matrice dans PROGRESS.md, « Décisions »).
 * Le serveur fait foi : ceci ne fait que masquer ce qu'il refuserait.
 * - gestion : créer / modifier affaires, pièces, devis, gammes, versions, bibliothèque, lots, ratios,
 *   liens d'espace client, supprimer des éléments de visite, envoyer un avenant ou saisir la décision du client ;
 * - montantsInternes : marge, déboursé sec, bilan investisseur, estimation, montants du tableau de bord.
 * Le conducteur de travaux n'a ni l'un ni l'autre.
 */
export function droits(me: Me) {
  const conducteur = me.roleEntreprise === 'CONDUCTEUR_TRAVAUX'
  return { gestion: !conducteur, montantsInternes: !conducteur }
}

export function useDroits() {
  return droits(useSession().me)
}
