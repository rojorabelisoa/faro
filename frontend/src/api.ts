import { apiUrl } from './config'

export interface Organisation {
  id: string
  nom: string
}

export interface Me {
  id: string
  subject: string
  email: string | null
  displayName: string | null
  roles: string[]
  organisation: Organisation
  roleEntreprise: RoleEntreprise
}

export type RoleEntreprise = 'GERANT' | 'CHARGE_ETUDES' | 'CONDUCTEUR_TRAVAUX'

export type Plan = 'ESSAI' | 'STARTER' | 'PRO' | 'BUSINESS'

export interface Entreprise {
  id: string
  nom: string
  membres: { id: string; nom: string | null; email: string | null; role: RoleEntreprise }[]
  /** maxUtilisateurs null : illimité ; essaiFin seulement pendant l'essai. */
  offre: { plan: Plan; libelle: string; maxUtilisateurs: number | null; essaiFin: string | null }
}

export interface OrganisationAdmin {
  id: string
  nom: string
  plan: Plan
  essaiFin: string | null
  createdAt: string
  utilisateurs: number
  affaires: number
  derniereActivite: string | null
}

export interface Invitation {
  id: string
  code: string
  role: RoleEntreprise
  email: string | null
  expiresAt: string
}

export type TypeBien = 'APPARTEMENT' | 'MAISON' | 'EXTENSION' | 'LOCAL_TERTIAIRE' | 'AUTRE'
export type StatutAffaire = 'VISITE' | 'ESTIMATION' | 'DOSSIER_TRAVAUX' | 'CHANTIER' | 'TERMINEE' | 'PERDUE'

export interface Affaire {
  id: string
  nom: string
  clientNom: string | null
  clientEmail: string | null
  clientTelephone: string | null
  adresse: string | null
  typeBien: TypeBien
  statut: StatutAffaire
  notes: string | null
  /** Version figée transmise au chantier (null avant le passage en chantier). */
  versionChantierId: string | null
  createdAt: string
  updatedAt: string
}

export interface Version {
  id: string
  numero: number
  libelle: string
  totalHt: number
  totalTtc: number
  auteur: string | null
  createdAt: string
  chantier: boolean
}

export interface AffaireInput {
  nom: string
  clientNom?: string | null
  clientEmail?: string | null
  clientTelephone?: string | null
  adresse?: string | null
  typeBien: TypeBien
  statut?: StatutAffaire
  notes?: string | null
}

export interface Piece {
  id: string
  nom: string
  surfaceM2: number | null
  perimetreMl: number | null
  hauteurM: number | null
  /** Calculée par le serveur : périmètre × hauteur. */
  surfaceMursM2: number | null
  typePiece: TypePiece | null
  niveauTravaux: NiveauTravaux | null
  ordre: number
}

export interface Estimation {
  pieces: {
    pieceId: string
    nom: string
    typePiece: TypePiece | null
    niveauTravaux: NiveauTravaux | null
    surfaceM2: number | null
    minHt: number | null
    maxHt: number | null
    manque: string | null
  }[]
  totalMinHt: number
  totalMaxHt: number
  devisHt: number
}

export interface Ratio {
  id: string
  typePiece: TypePiece
  niveauTravaux: NiveauTravaux
  prixMinM2: number
  prixMaxM2: number
  constate: { pieces: number; minM2: number; medianeM2: number; maxM2: number } | null
}

export type TypePiece = 'CUISINE' | 'SALLE_DE_BAINS' | 'WC' | 'SEJOUR' | 'CHAMBRE' | 'ENTREE_DEGAGEMENT'
  | 'BUANDERIE_CELLIER' | 'AUTRE'
export type NiveauTravaux = 'RAFRAICHISSEMENT' | 'RENOVATION' | 'RENOVATION_LOURDE'

export interface PieceInput {
  nom: string
  surfaceM2: number | null
  perimetreMl: number | null
  hauteurM: number | null
  typePiece: TypePiece | null
  niveauTravaux: NiveauTravaux | null
  ordre?: number
}

export interface LotInput {
  code: string
  nom: string
  ordre?: number
}

export interface Lot {
  id: string
  code: string
  nom: string
  ordre: number
}

export type Gamme = 'ECO' | 'STANDARD' | 'PREMIUM'

export interface Ligne {
  id: string
  pieceId: string | null
  lotId: string
  ouvrageId: string | null
  designation: string
  description: string | null
  unite: string
  quantite: number
  prixUnitaireHt: number
  tauxTva: number
  montantHt: number
  /** null : ligne commune à toutes les gammes. */
  gamme: Gamme | null
  /** Coût de revient unitaire HT prévu (facultatif). */
  debourseUnitaireHt: number | null
  /** false : option d'une gamme non retenue, hors totaux. */
  incluse: boolean
  ordre: number
}

export interface LigneInput {
  pieceId: string | null
  lotId: string
  designation: string
  description?: string | null
  unite: string
  quantite: number
  prixUnitaireHt: number
  tauxTva: number
  gamme?: Gamme | null
  debourseUnitaireHt?: number | null
}

export interface Devis {
  lignes: Ligne[]
  parPiece: {
    pieceId: string | null
    nom: string
    totalHt: number
    gammeChoisie: Gamme
    /** Total HT de la pièce pour chaque gamme ; null si elle n'a pas de ligne d'option. */
    gammes: Record<Gamme, number> | null
  }[]
  parLot: { lotId: string; code: string; nom: string; totalHt: number }[]
  tva: { taux: number; baseHt: number; montant: number }[]
  totalHt: number
  totalTva: number
  totalTtc: number
  totauxParGamme: Record<Gamme, number> | null
}

export interface Ouvrage {
  id: string
  lotId: string
  designation: string
  description: string | null
  unite: string
  prixUnitaireHt: number
  tauxTva: number
  debourseUnitaireHt: number | null
}

export type OuvrageInput = Omit<Ouvrage, 'id'>

export interface Observation {
  id: string
  pieceId: string | null
  texte: string
  auteur: string | null
  createdAt: string
}

export interface PhotoInfo {
  id: string
  pieceId: string | null
  legende: string | null
  contentType: string
  taille: number
  createdAt: string
}

export interface Visite {
  observations: Observation[]
  photos: PhotoInfo[]
}

export type StatutAvenant = 'BROUILLON' | 'ENVOYE' | 'VALIDE' | 'REFUSE'

export interface Totaux {
  totalHt: number
  tva: { taux: number; baseHt: number; montant: number }[]
  totalTva: number
  totalTtc: number
}

export interface Avenant {
  id: string
  numero: number
  objet: string
  motif: string | null
  statut: StatutAvenant
  lignes: Ligne[]
  totaux: Totaux
  createdAt: string
  envoyeAt: string | null
  decideAt: string | null
  decidePar: string | null
  decisionCommentaire: string | null
}

export interface Marche {
  chantier: boolean
  initialHt: number
  initialTtc: number
  avenantsValidesHt: number
  avenantsValidesTtc: number
  actuelHt: number
  actuelTtc: number
  avenantsEnAttenteHt: number
}

export type NatureDepense = 'MATERIAUX' | 'MAIN_OEUVRE' | 'SOUS_TRAITANCE' | 'LOCATION' | 'AUTRE'

export interface Depense {
  id: string
  lotId: string
  nature: NatureDepense
  libelle: string
  fournisseur: string | null
  montantHt: number
  dateDepense: string
}

export type DepenseInput = Omit<Depense, 'id'>

export interface Marge {
  lots: {
    lotId: string
    code: string
    nom: string
    venduHt: number
    coutPrevuHt: number
    coutReelHt: number
    margePrevueHt: number
    margeReelleHt: number
    lignesSansDebourse: number
  }[]
  total: {
    venduHt: number
    coutPrevuHt: number
    coutReelHt: number
    margePrevueHt: number
    margeReelleHt: number
    tauxMargePrevue: number | null
    tauxMargeReelle: number | null
    lignesSansDebourse: number
  }
  depenses: Depense[]
}

export interface EspaceClient {
  entreprise: string
  affaire: string
  adresse: string | null
  statut: StatutAffaire
  chantier: boolean
  devis: Devis
  lots: { id: string; code: string; nom: string }[]
  avenants: Avenant[]
  marche: Marche
}

export interface LienClient {
  id: string
  code: string
  createdAt: string
  derniereVisite: string | null
}

/** API de l'espace client : sans compte, le code secret du lien fait office d'accès. */
export function createPublicApi(code: string) {
  const base = `/api/public/client/${encodeURIComponent(code)}`
  const appel = async <T>(path: string, init: RequestInit = {}): Promise<T> => {
    const response = await fetch(`${apiUrl}${base}${path}`, {
      ...init,
      headers: init.body ? { 'Content-Type': 'application/json' } : undefined,
    })
    if (!response.ok) {
      let message = response.status === 404 ? 'Ce lien n\'est plus valide. Demandez-en un nouveau à votre entreprise.'
        : `Erreur ${response.status}`
      try {
        const problem = await response.json()
        if (problem?.detail && response.status !== 404) message = problem.detail
      } catch {
        // corps vide
      }
      throw new ApiError(response.status, message)
    }
    if (response.status === 204) return undefined as T
    return (response.headers.get('Content-Type')?.includes('json') ? response.json() : response.blob()) as Promise<T>
  }
  return {
    espace: () => appel<EspaceClient>(''),
    choisirGamme: (pieceId: string | null, gamme: Gamme) =>
      appel<void>(pieceId ? `/pieces/${pieceId}/gamme` : '/gamme', json('PUT', { gamme })),
    decider: (avenantId: string, input: { decision: 'VALIDE' | 'REFUSE'; nom: string; commentaire: string | null }) =>
      appel<void>(`/avenants/${avenantId}/decision`, json('POST', input)),
    devisPdf: () => appel<Blob>('/devis.pdf'),
    avenantPdf: (avenantId: string) => appel<Blob>(`/avenants/${avenantId}/document.pdf`),
  }
}

export interface ImportResultat {
  importes: number
  misAJour: number
  erreurs: { ligne: number; message: string }[]
}

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(token: string, path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${apiUrl}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
    },
  })
  if (!response.ok) {
    let message = `Erreur API ${response.status}`
    try {
      const problem = await response.json()
      if (problem?.detail) message = problem.detail
    } catch {
      // corps vide ou non JSON : on garde le message générique
    }
    throw new ApiError(response.status, message)
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

async function download(token: string, path: string): Promise<Blob> {
  const response = await fetch(`${apiUrl}${path}`, { headers: { Authorization: `Bearer ${token}` } })
  if (!response.ok) throw new ApiError(response.status, `Erreur API ${response.status}`)
  return response.blob()
}

/** Import tout-ou-rien : la réponse (200 ou 400) détaille les erreurs ligne par ligne. */
async function upload<T>(token: string, path: string, form: FormData): Promise<T> {
  const response = await fetch(`${apiUrl}${path}`, {
    method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: form,
  })
  if (!response.ok) {
    let message = `Erreur API ${response.status}`
    try {
      const problem = await response.json()
      if (problem?.detail) message = problem.detail
    } catch {
      // corps vide ou non JSON
    }
    throw new ApiError(response.status, message)
  }
  return response.json()
}

async function importCsv(token: string, fichier: File): Promise<ImportResultat> {
  const form = new FormData()
  form.append('fichier', fichier)
  const response = await fetch(`${apiUrl}/api/ouvrages/import`, {
    method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: form,
  })
  if (response.ok || response.status === 400) return response.json()
  throw new ApiError(response.status, `Erreur API ${response.status}`)
}

const json = (method: string, body: unknown): RequestInit => ({ method, body: JSON.stringify(body) })

/** Client de l'API faro, lié au jeton d'accès de l'utilisateur connecté. */
export function createApi(token: string) {
  return {
    me: () => request<Me>(token, '/api/me'),

    entreprise: () => request<Entreprise>(token, '/api/entreprise'),
    renommerEntreprise: (nom: string) => request<void>(token, '/api/entreprise', json('PUT', { nom })),
    changerRole: (membreId: string, role: RoleEntreprise) =>
      request<void>(token, `/api/entreprise/membres/${membreId}/role`, json('PUT', { role })),
    retirerMembre: (membreId: string) =>
      request<void>(token, `/api/entreprise/membres/${membreId}`, { method: 'DELETE' }),
    invitations: () => request<Invitation[]>(token, '/api/entreprise/invitations'),
    inviter: (role: RoleEntreprise, email: string | null) =>
      request<Invitation>(token, '/api/entreprise/invitations', json('POST', { role, email })),
    revoquerInvitation: (id: string) =>
      request<void>(token, `/api/entreprise/invitations/${id}`, { method: 'DELETE' }),
    invitationInfo: (code: string) =>
      request<{ entreprise: string; role: RoleEntreprise; utilisable: boolean }>(token, `/api/invitations/${code}`),
    accepterInvitation: (code: string) =>
      request<void>(token, `/api/invitations/${code}/accepter`, { method: 'POST' }),

    adminOrganisations: () => request<OrganisationAdmin[]>(token, '/api/admin/organisations'),
    adminChangerPlan: (id: string, plan: Plan) =>
      request<void>(token, `/api/admin/organisations/${id}/plan`, json('PUT', { plan })),

    affaires: () => request<Affaire[]>(token, '/api/affaires'),
    affaire: (id: string) => request<Affaire>(token, `/api/affaires/${id}`),
    createAffaire: (input: AffaireInput) => request<Affaire>(token, '/api/affaires', json('POST', input)),
    updateAffaire: (id: string, input: AffaireInput) =>
      request<Affaire>(token, `/api/affaires/${id}`, json('PUT', input)),
    deleteAffaire: (id: string) => request<void>(token, `/api/affaires/${id}`, { method: 'DELETE' }),

    pieces: (affaireId: string) => request<Piece[]>(token, `/api/affaires/${affaireId}/pieces`),
    createPiece: (affaireId: string, input: PieceInput) =>
      request<Piece>(token, `/api/affaires/${affaireId}/pieces`, json('POST', input)),
    updatePiece: (affaireId: string, id: string, input: PieceInput) =>
      request<Piece>(token, `/api/affaires/${affaireId}/pieces/${id}`, json('PUT', input)),
    deletePiece: (affaireId: string, id: string) =>
      request<void>(token, `/api/affaires/${affaireId}/pieces/${id}`, { method: 'DELETE' }),

    lots: () => request<Lot[]>(token, '/api/lots'),
    createLot: (input: LotInput) => request<Lot>(token, '/api/lots', json('POST', input)),
    updateLot: (id: string, input: LotInput) => request<Lot>(token, `/api/lots/${id}`, json('PUT', input)),
    deleteLot: (id: string) => request<void>(token, `/api/lots/${id}`, { method: 'DELETE' }),

    devis: (affaireId: string) => request<Devis>(token, `/api/affaires/${affaireId}/devis`),
    devisCsv: (affaireId: string) => download(token, `/api/affaires/${affaireId}/devis.csv`),
    devisPdf: (affaireId: string, vue: 'PIECE' | 'LOT') =>
      download(token, `/api/affaires/${affaireId}/devis.pdf?vue=${vue}`),
    createLigne: (affaireId: string, input: LigneInput) =>
      request<Ligne>(token, `/api/affaires/${affaireId}/lignes`, json('POST', input)),
    createLigneDepuisOuvrage: (affaireId: string, input: { ouvrageId: string; pieceId: string | null; quantite: number }) =>
      request<Ligne>(token, `/api/affaires/${affaireId}/lignes/depuis-ouvrage`, json('POST', input)),
    updateLigne: (affaireId: string, id: string, input: LigneInput) =>
      request<Ligne>(token, `/api/affaires/${affaireId}/lignes/${id}`, json('PUT', input)),
    choisirGamme: (affaireId: string, pieceId: string | null, gamme: Gamme) =>
      request<void>(token, pieceId ? `/api/affaires/${affaireId}/pieces/${pieceId}/gamme` : `/api/affaires/${affaireId}/gamme`,
        json('PUT', { gamme })),
    reorderLignes: (affaireId: string, ids: string[]) =>
      request<void>(token, `/api/affaires/${affaireId}/lignes/ordre`, json('PUT', { ids })),
    deleteLigne: (affaireId: string, id: string) =>
      request<void>(token, `/api/affaires/${affaireId}/lignes/${id}`, { method: 'DELETE' }),

    ouvrages: (q?: string) =>
      request<Ouvrage[]>(token, `/api/ouvrages${q ? `?q=${encodeURIComponent(q)}` : ''}`),
    createOuvrage: (input: OuvrageInput) => request<Ouvrage>(token, '/api/ouvrages', json('POST', input)),
    updateOuvrage: (id: string, input: OuvrageInput) =>
      request<Ouvrage>(token, `/api/ouvrages/${id}`, json('PUT', input)),
    visite: (affaireId: string) => request<Visite>(token, `/api/affaires/${affaireId}/visite`),
    visitePdf: (affaireId: string) => download(token, `/api/affaires/${affaireId}/visite.pdf`),
    addObservation: (affaireId: string, input: { pieceId: string | null; texte: string }) =>
      request<Observation>(token, `/api/affaires/${affaireId}/observations`, json('POST', input)),
    deleteObservation: (affaireId: string, id: string) =>
      request<void>(token, `/api/affaires/${affaireId}/observations/${id}`, { method: 'DELETE' }),
    addPhoto: (affaireId: string, fichier: Blob, pieceId: string | null, legende: string) => {
      const form = new FormData()
      form.append('fichier', fichier, 'photo.jpg')
      if (pieceId) form.append('pieceId', pieceId)
      if (legende) form.append('legende', legende)
      return upload<PhotoInfo>(token, `/api/affaires/${affaireId}/photos`, form)
    },
    photo: (affaireId: string, id: string) => download(token, `/api/affaires/${affaireId}/photos/${id}`),
    deletePhoto: (affaireId: string, id: string) =>
      request<void>(token, `/api/affaires/${affaireId}/photos/${id}`, { method: 'DELETE' }),

    estimation: (affaireId: string) => request<Estimation>(token, `/api/affaires/${affaireId}/estimation`),
    ratios: () => request<Ratio[]>(token, '/api/ratios'),
    updateRatio: (id: string, input: { prixMinM2: number; prixMaxM2: number }) =>
      request<void>(token, `/api/ratios/${id}`, json('PUT', input)),

    versions: (affaireId: string) => request<Version[]>(token, `/api/affaires/${affaireId}/versions`),
    figerVersion: (affaireId: string, libelle: string) =>
      request<Version>(token, `/api/affaires/${affaireId}/versions`, json('POST', { libelle })),
    passerEnChantier: (affaireId: string) =>
      request<Version>(token, `/api/affaires/${affaireId}/chantier`, json('POST', {})),
    versionPdf: (affaireId: string, id: string, vue: 'PIECE' | 'LOT') =>
      download(token, `/api/affaires/${affaireId}/versions/${id}/devis.pdf?vue=${vue}`),

    avenants: (affaireId: string) => request<Avenant[]>(token, `/api/affaires/${affaireId}/avenants`),
    marche: (affaireId: string) => request<Marche>(token, `/api/affaires/${affaireId}/marche`),
    createAvenant: (affaireId: string, input: { objet: string; motif: string | null }) =>
      request<Avenant>(token, `/api/affaires/${affaireId}/avenants`, json('POST', input)),
    deleteAvenant: (affaireId: string, id: string) =>
      request<void>(token, `/api/affaires/${affaireId}/avenants/${id}`, { method: 'DELETE' }),
    addLigneAvenant: (affaireId: string, id: string, input: LigneInput) =>
      request<Ligne>(token, `/api/affaires/${affaireId}/avenants/${id}/lignes`, json('POST', input)),
    envoyerAvenant: (affaireId: string, id: string) =>
      request<Avenant>(token, `/api/affaires/${affaireId}/avenants/${id}/envoyer`, { method: 'POST' }),
    deciderAvenant: (affaireId: string, id: string, input: { decision: 'VALIDE' | 'REFUSE'; par: string; commentaire: string | null }) =>
      request<Avenant>(token, `/api/affaires/${affaireId}/avenants/${id}/decision`, json('POST', input)),
    avenantPdf: (affaireId: string, id: string) =>
      download(token, `/api/affaires/${affaireId}/avenants/${id}/document.pdf`),

    marge: (affaireId: string) => request<Marge>(token, `/api/affaires/${affaireId}/marge`),
    addDepense: (affaireId: string, input: DepenseInput) =>
      request<Depense>(token, `/api/affaires/${affaireId}/depenses`, json('POST', input)),
    deleteDepense: (affaireId: string, id: string) =>
      request<void>(token, `/api/affaires/${affaireId}/depenses/${id}`, { method: 'DELETE' }),

    liensClient: (affaireId: string) => request<LienClient[]>(token, `/api/affaires/${affaireId}/acces-client`),
    creerLienClient: (affaireId: string) =>
      request<LienClient>(token, `/api/affaires/${affaireId}/acces-client`, { method: 'POST' }),
    revoquerLienClient: (affaireId: string, id: string) =>
      request<void>(token, `/api/affaires/${affaireId}/acces-client/${id}`, { method: 'DELETE' }),

    exportOuvrages: () => download(token, '/api/ouvrages/export.csv'),
    importOuvrages: (fichier: File) => importCsv(token, fichier),
    deleteOuvrage: (id: string) => request<void>(token, `/api/ouvrages/${id}`, { method: 'DELETE' }),
  }
}

export type Api = ReturnType<typeof createApi>
