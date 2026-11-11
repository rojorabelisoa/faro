import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import EspaceClientPage from './EspaceClientPage'

const ESPACE = {
  entreprise: 'Rénov Plus',
  affaire: 'T3 Croix-Rousse',
  adresse: null,
  statut: 'ESTIMATION',
  chantier: false,
  lots: [{ id: 'l10', code: '10', nom: 'Carrelage' }],
  devis: {
    lignes: [
      { id: 'a', pieceId: 'sdb', lotId: 'l10', ouvrageId: null, designation: 'Faïence simple', description: null,
        unite: 'U', quantite: 1, prixUnitaireHt: 500, tauxTva: 10, montantHt: 500, gamme: 'ECO',
        debourseUnitaireHt: null, incluse: false, ordre: 1 },
      { id: 'b', pieceId: 'sdb', lotId: 'l10', ouvrageId: null, designation: 'Faïence rectifiée', description: null,
        unite: 'U', quantite: 1, prixUnitaireHt: 800, tauxTva: 10, montantHt: 800, gamme: 'STANDARD',
        debourseUnitaireHt: null, incluse: true, ordre: 2 },
    ],
    parPiece: [{ pieceId: 'sdb', nom: 'Salle de bains', totalHt: 800, gammeChoisie: 'STANDARD',
      gammes: { ECO: 500, STANDARD: 800, PREMIUM: 1200 } }],
    parLot: [], tva: [], totalHt: 800, totalTva: 80, totalTtc: 880,
    totauxParGamme: { ECO: 500, STANDARD: 800, PREMIUM: 1200 },
  },
  avenants: [],
  marche: { chantier: false, initialHt: 800, initialTtc: 880, avenantsValidesHt: 0, avenantsValidesTtc: 0,
    actuelHt: 800, actuelTtc: 880, avenantsEnAttenteHt: 0 },
}

describe('EspaceClientPage', () => {
  afterEach(() => vi.restoreAllMocks())

  it('montre au client uniquement les lignes retenues et lui permet de changer de finition', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (_url, init) =>
      init?.method === 'PUT' ? new Response(null, { status: 204 })
        : new Response(JSON.stringify(ESPACE), { headers: { 'Content-Type': 'application/json' } }))

    render(<EspaceClientPage code="secret" />)

    expect(await screen.findByRole('heading', { name: 'T3 Croix-Rousse' })).toBeInTheDocument()
    expect(screen.getByText('Faïence rectifiée')).toBeInTheDocument()
    expect(screen.queryByText('Faïence simple')).not.toBeInTheDocument()

    await userEvent.click(within(screen.getByRole('group', { name: 'Finition Salle de bains' }))
      .getByRole('button', { name: /Eco/ }))

    expect(fetchSpy).toHaveBeenCalledWith('http://localhost:8080/api/public/client/secret/pieces/sdb/gamme',
      expect.objectContaining({ method: 'PUT', body: JSON.stringify({ gamme: 'ECO' }) }))
  })

  it('explique quand le lien n’est plus valide', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 404 }))
    render(<EspaceClientPage code="revoque" />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Ce lien n\'est plus valide')
  })
})
