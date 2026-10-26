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
