import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import type { Api, Devis } from '../api'
import { SessionContext } from '../ApiContext'
import AffairePage from './AffairePage'

const LOTS = [
  { id: 'l01', code: '01', nom: 'Installation de chantier', ordre: 1 },
  { id: 'l07', code: '07', nom: 'Plomberie', ordre: 7 },
  { id: 'l10', code: '10', nom: 'Carrelage', ordre: 10 },
]
const PIECES = [
  { id: 'cuisine', nom: 'Cuisine', surfaceM2: 12, ordre: 1 },
  { id: 'sdb', nom: 'Salle de bains', surfaceM2: null, ordre: 2 },
]
const ligne = (id: string, pieceId: string | null, lotId: string, designation: string, montantHt: number) => ({
  id, pieceId, lotId, ouvrageId: null, designation, description: null, unite: 'U', quantite: 1,
  prixUnitaireHt: montantHt, tauxTva: 10, montantHt, ordre: 1,
})
const DEVIS: Devis = {
  lignes: [
    ligne('1', null, 'l01', 'Protection des sols', 350),
    ligne('2', 'cuisine', 'l07', 'Évier', 420),
    ligne('3', 'cuisine', 'l10', 'Faïence', 552.5),
    ligne('4', 'sdb', 'l10', 'Carrelage sol', 307.7),
  ],
  parPiece: [
    { pieceId: null, nom: 'Général', totalHt: 350 },
    { pieceId: 'cuisine', nom: 'Cuisine', totalHt: 972.5 },
    { pieceId: 'sdb', nom: 'Salle de bains', totalHt: 307.7 },
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
}

function renderPage() {
  const api = {
    affaire: vi.fn().mockResolvedValue({ id: 'a1', nom: 'T3 Lyon', clientNom: 'M. Martin', adresse: null,
      typeBien: 'APPARTEMENT', statut: 'ESTIMATION' }),
    pieces: vi.fn().mockResolvedValue(PIECES),
    lots: vi.fn().mockResolvedValue(LOTS),
    devis: vi.fn().mockResolvedValue(DEVIS),
    ouvrages: vi.fn().mockResolvedValue([]),
    createLigne: vi.fn().mockResolvedValue({}),
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
      pieceId: 'sdb', lotId: 'l10', designation: 'Carrelage mural', unite: 'm²', quantite: 4.25,
      prixUnitaireHt: 72.4, tauxTva: 10,
    })
  })
})
