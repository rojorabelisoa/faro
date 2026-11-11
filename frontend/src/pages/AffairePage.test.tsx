import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import type { Api, Devis, Gamme, Piece } from '../api'
import { SessionContext } from '../ApiContext'
import AffairePage, { deplacer, metres } from './AffairePage'

const LOTS = [
  { id: 'l01', code: '01', nom: 'Installation de chantier', ordre: 1 },
  { id: 'l07', code: '07', nom: 'Plomberie', ordre: 7 },
  { id: 'l10', code: '10', nom: 'Carrelage', ordre: 10 },
]
const PIECES: Piece[] = [
  { id: 'cuisine', nom: 'Cuisine', surfaceM2: 12, perimetreMl: 14, hauteurM: 2.5, surfaceMursM2: 35, typePiece: 'CUISINE', niveauTravaux: 'RENOVATION', ordre: 1 },
  { id: 'sdb', nom: 'Salle de bains', surfaceM2: null, perimetreMl: null, hauteurM: null, surfaceMursM2: null, typePiece: null, niveauTravaux: null, ordre: 2 },
]
const ligne = (id: string, pieceId: string | null, lotId: string, designation: string, montantHt: number,
  gamme: Gamme | null = null, incluse = true) => ({
  id, pieceId, lotId, ouvrageId: null, designation, description: null, unite: 'U', quantite: 1,
  prixUnitaireHt: montantHt, tauxTva: 10, montantHt, gamme, debourseUnitaireHt: null, incluse, ordre: 1,
})
const DEVIS: Devis = {
  lignes: [
    ligne('1', null, 'l01', 'Protection des sols', 350),
    ligne('2', 'cuisine', 'l07', 'Évier', 420),
    ligne('3', 'cuisine', 'l10', 'Faïence', 552.5),
    ligne('4', 'sdb', 'l10', 'Carrelage sol', 307.7),
  ],
  parPiece: [
    { pieceId: null, nom: 'Général', totalHt: 350, gammeChoisie: 'STANDARD', gammes: null },
    { pieceId: 'cuisine', nom: 'Cuisine', totalHt: 972.5, gammeChoisie: 'STANDARD', gammes: null },
    { pieceId: 'sdb', nom: 'Salle de bains', totalHt: 307.7, gammeChoisie: 'STANDARD', gammes: null },
  ],
  parLot: [
    { lotId: 'l01', code: '01', nom: 'Installation de chantier', totalHt: 350 },
    { lotId: 'l07', code: '07', nom: 'Plomberie', totalHt: 420 },
    { lotId: 'l10', code: '10', nom: 'Carrelage', totalHt: 860.2 },
  ],
  tva: [{ taux: 10, baseHt: 1630.2, montant: 163.02 }],
  totalHt: 1630.2,
  totalTva: 163.02,
  totalTtc: 1793.22,
  totauxParGamme: null,
}

function renderPage(devis: Devis = DEVIS) {
  const api = {
    affaire: vi.fn().mockResolvedValue({ id: 'a1', nom: 'T3 Lyon', clientNom: 'M. Martin', adresse: null,
      typeBien: 'APPARTEMENT', statut: 'ESTIMATION' }),
    pieces: vi.fn().mockResolvedValue(PIECES),
    lots: vi.fn().mockResolvedValue(LOTS),
    devis: vi.fn().mockResolvedValue(devis),
    choisirGamme: vi.fn().mockResolvedValue(undefined),
    versions: vi.fn().mockResolvedValue([]),
    marge: vi.fn().mockResolvedValue({ lots: [], total: { venduHt: 0, coutPrevuHt: 0, coutReelHt: 0, margePrevueHt: 0, margeReelleHt: 0, tauxMargePrevue: null, tauxMargeReelle: null, lignesSansDebourse: 0 }, depenses: [] }),
    ouvrages: vi.fn().mockResolvedValue([]),
    createLigne: vi.fn().mockResolvedValue({}),
    visite: vi.fn().mockResolvedValue({ observations: [], photos: [] }),
    estimation: vi.fn().mockResolvedValue({
      pieces: [
        { pieceId: 'cuisine', nom: 'Cuisine', typePiece: 'CUISINE', niveauTravaux: 'RENOVATION', surfaceM2: 12,
          minHt: 8400, maxHt: 15600, manque: null },
        { pieceId: 'sdb', nom: 'Salle de bains', typePiece: null, niveauTravaux: null, surfaceM2: null,
          minHt: null, maxHt: null, manque: 'surface' },
      ],
      totalMinHt: 8400, totalMaxHt: 15600, devisHt: 1630.2,
    }),
  }
  const me = { id: '1', subject: 's', email: null, displayName: 'Demo', roles: [], organisation: { id: 'o', nom: 'X' } }
  render(
    <SessionContext.Provider value={{ api: api as unknown as Api, me }}>
      <MemoryRouter initialEntries={['/affaires/a1']}>
        <Routes>
          <Route path="/affaires/:id" element={<AffairePage />} />
        </Routes>
      </MemoryRouter>
    </SessionContext.Provider>,
  )
  return api
}

// Intl (fr-FR) sépare les milliers et le symbole € par des espaces insécables.
const texte = (valeur: string | null) => (valeur ?? '').replace(/[\u00a0\u202f]/g, ' ')

const groupes = () => screen.getAllByRole('row').filter((row) => row.classList.contains('groupe'))
  .map((row) => texte(row.textContent))

describe('AffairePage', () => {
  it('présente le même devis par pièce puis par lot', async () => {
    renderPage()

    expect(await screen.findByRole('heading', { name: 'T3 Lyon' })).toBeInTheDocument()
    expect(groupes()).toEqual(['Général350,00 €', 'Cuisine972,50 €', 'Salle de bains307,70 €'])

    await userEvent.click(screen.getByRole('button', { name: 'Par lot' }))

    expect(groupes()).toEqual([
      '01 · Installation de chantier350,00 €', '07 · Plomberie420,00 €', '10 · Carrelage860,20 €',
    ])
    const total = screen.getAllByRole('row').find((row) => row.classList.contains('total'))
    expect(texte(total?.textContent ?? null)).toBe('Total TTC1 793,22 €')
  })

  it('ajoute une ligne dans la pièce choisie, avec une quantité saisie à la française', async () => {
    const api = renderPage()
    const form = await screen.findByRole('form', { name: 'Ajouter une ligne' })

    await userEvent.selectOptions(within(form).getByLabelText('Pièce'), 'sdb')
    await userEvent.type(within(form).getByLabelText('Quantité'), '4,25')
    await userEvent.type(within(form).getByLabelText('Désignation'), 'Carrelage mural')
    await userEvent.selectOptions(within(form).getByLabelText('Lot'), 'l10')
    await userEvent.selectOptions(within(form).getByLabelText('Unité'), 'm²')
    await userEvent.type(within(form).getByLabelText('PU HT (€)'), '72,4')
    await userEvent.click(within(form).getByRole('button', { name: 'Ajouter la ligne' }))

    expect(api.createLigne).toHaveBeenCalledWith('a1', {
      pieceId: 'sdb', lotId: 'l10', designation: 'Carrelage mural', description: null, unite: 'm²', quantite: 4.25,
      prixUnitaireHt: 72.4, tauxTva: 10, gamme: null, debourseUnitaireHt: null,
    })
  })
})

describe('deplacer', () => {
  it('échange une ligne avec sa voisine du même groupe, même si d’autres lignes sont entre les deux', () => {
    const lignes = DEVIS.lignes
    const cuisine = lignes.filter((l) => l.pieceId === 'cuisine')

    expect(deplacer(lignes, cuisine, cuisine[1], -1)).toEqual(['1', '3', '2', '4'])
    expect(deplacer(lignes, cuisine, cuisine[0], -1)).toBeNull()
    expect(deplacer(lignes, cuisine, cuisine[1], 1)).toBeNull()
  })
})

describe('metres', () => {
  it('propose sol, murs, plafond et périmètre quand la pièce est mesurée', () => {
    expect(metres(PIECES[0]).map((m) => `${m.libelle} ${m.quantite} ${m.unite}`))
      .toEqual(['Sol 12 m²', 'Murs 35 m²', 'Plafond 12 m²', 'Périmètre 14 ml'])
    expect(metres(PIECES[1])).toEqual([])
  })
})

describe('AffairePage – métrés', () => {
  it('remplit la quantité depuis les murs de la pièce choisie', async () => {
    renderPage()
    const form = await screen.findByRole('form', { name: 'Ajouter une ligne' })
    await userEvent.selectOptions(within(form).getByLabelText('Pièce'), 'cuisine')
    await userEvent.click(within(form).getByRole('button', { name: /^Murs 35/ }))

    expect(within(form).getByLabelText('Quantité')).toHaveValue('35')
    expect(within(form).getByLabelText('Unité')).toHaveValue('m²')
  })
})

describe('AffairePage – estimation', () => {
  it('affiche la fourchette, les pièces incomplètes et compare au devis', async () => {
    renderPage()
    const estimation = await screen.findByRole('region', { name: 'Estimation rapide' })
    expect(texte(estimation.querySelector('.fourchette')?.textContent ?? null)).toBe('8 400,00 € – 15 600,00 € HT')
    expect(within(estimation).getByText('surface manquante')).toBeInTheDocument()
    expect(within(estimation).getByText(/hors de la fourchette/)).toBeInTheDocument()
  })
})

describe('AffairePage – gammes', () => {
  it('montre le prix de chaque gamme, grise les options non retenues et change de gamme', async () => {
    const avecOptions: Devis = {
      ...DEVIS,
      lignes: [
        ligne('a', 'sdb', 'l10', 'Carrelage 30x30', 500, 'ECO', false),
        ligne('b', 'sdb', 'l10', 'Carrelage 60x60', 800, 'STANDARD', true),
        ligne('c', 'sdb', 'l10', 'Grand format', 1400, 'PREMIUM', false),
      ],
      parPiece: [{ pieceId: 'sdb', nom: 'Salle de bains', totalHt: 800, gammeChoisie: 'STANDARD',
        gammes: { ECO: 500, STANDARD: 800, PREMIUM: 1400 } }],
      totauxParGamme: { ECO: 500, STANDARD: 800, PREMIUM: 1400 },
    }
    const api = renderPage(avecOptions)

    const choix = await screen.findByRole('group', { name: 'Gamme Salle de bains' })
    expect(within(choix).getByRole('button', { name: /Standard/ })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText('Grand format').closest('tr')).toHaveClass('non-incluse')
    expect(screen.getByText('Carrelage 60x60').closest('tr')).not.toHaveClass('non-incluse')

    await userEvent.click(within(choix).getByRole('button', { name: /Premium/ }))
    expect(api.choisirGamme).toHaveBeenCalledWith('a1', 'sdb', 'PREMIUM')
  })
})
