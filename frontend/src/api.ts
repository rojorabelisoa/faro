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
  ordre: number
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
    createPiece: (affaireId: string, input: { nom: string; surfaceM2: number | null }) =>
      request<Piece>(token, `/api/affaires/${affaireId}/pieces`, json('POST', input)),
    deletePiece: (affaireId: string, id: string) =>
      request<void>(token, `/api/affaires/${affaireId}/pieces/${id}`, { method: 'DELETE' }),

    lots: () => request<Lot[]>(token, '/api/lots'),

    devis: (affaireId: string) => request<Devis>(token, `/api/affaires/${affaireId}/devis`),
    createLigne: (affaireId: string, input: LigneInput) =>
      request<Ligne>(token, `/api/affaires/${affaireId}/lignes`, json('POST', input)),
    createLigneDepuisOuvrage: (affaireId: string, input: { ouvrageId: string; pieceId: string | null; quantite: number }) =>
      request<Ligne>(token, `/api/affaires/${affaireId}/lignes/depuis-ouvrage`, json('POST', input)),
    updateLigne: (affaireId: string, id: string, input: LigneInput) =>
      request<Ligne>(token, `/api/affaires/${affaireId}/lignes/${id}`, json('PUT', input)),
    deleteLigne: (affaireId: string, id: string) =>
      request<void>(token, `/api/affaires/${affaireId}/lignes/${id}`, { method: 'DELETE' }),

    ouvrages: (q?: string) =>
      request<Ouvrage[]>(token, `/api/ouvrages${q ? `?q=${encodeURIComponent(q)}` : ''}`),
    createOuvrage: (input: OuvrageInput) => request<Ouvrage>(token, '/api/ouvrages', json('POST', input)),
    deleteOuvrage: (id: string) => request<void>(token, `/api/ouvrages/${id}`, { method: 'DELETE' }),
  }
}

export type Api = ReturnType<typeof createApi>
