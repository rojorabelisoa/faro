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
  createdAt: string
  updatedAt: string
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
  ordre: number
}

export interface PieceInput {
  nom: string
  surfaceM2: number | null
  perimetreMl: number | null
  hauteurM: number | null
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
}

export interface Devis {
  lignes: Ligne[]
  parPiece: { pieceId: string | null; nom: string; totalHt: number }[]
  parLot: { lotId: string; code: string; nom: string; totalHt: number }[]
  tva: { taux: number; baseHt: number; montant: number }[]
  totalHt: number
  totalTva: number
  totalTtc: number
}

export interface Ouvrage {
  id: string
  lotId: string
  designation: string
  description: string | null
  unite: string
  prixUnitaireHt: number
  tauxTva: number
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
    devisPdf: (affaireId: string, vue: 'PIECE' | 'LOT') =>
      download(token, `/api/affaires/${affaireId}/devis.pdf?vue=${vue}`),
    createLigne: (affaireId: string, input: LigneInput) =>
      request<Ligne>(token, `/api/affaires/${affaireId}/lignes`, json('POST', input)),
    createLigneDepuisOuvrage: (affaireId: string, input: { ouvrageId: string; pieceId: string | null; quantite: number }) =>
      request<Ligne>(token, `/api/affaires/${affaireId}/lignes/depuis-ouvrage`, json('POST', input)),
    updateLigne: (affaireId: string, id: string, input: LigneInput) =>
      request<Ligne>(token, `/api/affaires/${affaireId}/lignes/${id}`, json('PUT', input)),
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

    exportOuvrages: () => download(token, '/api/ouvrages/export.csv'),
    importOuvrages: (fichier: File) => importCsv(token, fichier),
    deleteOuvrage: (id: string) => request<void>(token, `/api/ouvrages/${id}`, { method: 'DELETE' }),
  }
}

export type Api = ReturnType<typeof createApi>
