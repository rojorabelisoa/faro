const euros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' })
const nombre = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 3 })

export const formatEuros = (montant: number) => euros.format(montant)
export const formatNombre = (valeur: number) => nombre.format(valeur)
export const formatTaux = (taux: number) => `${nombre.format(taux)} %`

/** Accepte « 8,5 » comme « 8.5 ». Renvoie NaN si la saisie n'est pas un nombre. */
export function parseNombre(saisie: string): number {
  const normalise = saisie.replace(/\s/g, '').replace(',', '.')
  return normalise === '' ? NaN : Number(normalise)
}

export const LIBELLES_TYPE_BIEN: Record<string, string> = {
  APPARTEMENT: 'Appartement',
  MAISON: 'Maison',
  EXTENSION: 'Extension',
  LOCAL_TERTIAIRE: 'Local tertiaire',
  AUTRE: 'Autre',
}

export const LIBELLES_STATUT: Record<string, string> = {
  VISITE: 'Visite',
  ESTIMATION: 'Estimation',
  DOSSIER_TRAVAUX: 'Dossier travaux',
  CHANTIER: 'Chantier',
  TERMINEE: 'Terminée',
  PERDUE: 'Perdue',
}

export const UNITES = ['U', 'm²', 'ml', 'm³', 'forfait', 'h', 'ens']
export const TAUX_TVA = [10, 5.5, 20, 0]

export const LIBELLES_TYPE_PIECE: Record<string, string> = {
  CUISINE: 'Cuisine',
  SALLE_DE_BAINS: 'Salle de bains',
  WC: 'WC',
  SEJOUR: 'Séjour / salon',
  CHAMBRE: 'Chambre',
  ENTREE_DEGAGEMENT: 'Entrée / dégagement',
  BUANDERIE_CELLIER: 'Buanderie / cellier',
  AUTRE: 'Autre',
}

export const LIBELLES_NIVEAU: Record<string, string> = {
  RAFRAICHISSEMENT: 'Rafraîchissement',
  RENOVATION: 'Rénovation',
  RENOVATION_LOURDE: 'Rénovation lourde',
}

/** Devine le type de pièce à partir de son nom (« SDB étage » → salle de bains). */
export function devinerTypePiece(nom: string): string | null {
  const n = nom.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
  if (/cuisine|kitchenette/.test(n)) return 'CUISINE'
  if (/sdb|salle d.?eau|salle de bain|douche/.test(n)) return 'SALLE_DE_BAINS'
  if (/\bwc\b|toilette/.test(n)) return 'WC'
  if (/sejour|salon|living|salle a manger/.test(n)) return 'SEJOUR'
  if (/chambre|bureau/.test(n)) return 'CHAMBRE'
  if (/entree|couloir|degagement|palier|hall/.test(n)) return 'ENTREE_DEGAGEMENT'
  if (/buanderie|cellier|cave|dressing/.test(n)) return 'BUANDERIE_CELLIER'
  return null
}
