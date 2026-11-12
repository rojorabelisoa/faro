import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeAll, describe, expect, it, vi } from 'vitest'
import type { Api, TableauDeBord } from '../api'
import { SessionContext } from '../ApiContext'
import TableauDeBordPage from './TableauDeBordPage'

// jsdom n'a pas IntersectionObserver (utilisé pour lancer les compteurs à l'apparition).
beforeAll(() => {
  class Observateur {
    constructor(private rappel: IntersectionObserverCallback) {}
    observe(cible: Element) {
      this.rappel([{ isIntersecting: true, target: cible } as IntersectionObserverEntry], this as never)
    }
    unobserve() {}
    disconnect() {}
  }
  vi.stubGlobal('IntersectionObserver', Observateur)
})

const indicateur = (valeur: number | null, detail: string, serie: number[] = [], tendance: number | null = null) =>
  ({ valeur, tendance, serie, detail })

const TABLEAU: TableauDeBord = {
  affairesActives: indicateur(3, '1 créée(s) ce mois-ci', [0, 1, 0, 2, 1, 1]),
  signeCeMois: indicateur(10500, 'dossiers travaux et avenants', [0, 0, 0, 0, 4000, 10500], 162.5),
  margeReelle: indicateur(42.9, 'sur les chantiers avec dépenses'),
  enAttenteClient: indicateur(300, '1 avenant(s) envoyé(s)'),
  transformation: { taux: 50, gagnees: 1, perdues: 1, enCours: 2 },
  signeParMois: Array.from({ length: 12 }, (_, i) => ({ mois: `2026-${String(i + 1).padStart(2, '0')}`, montantHt: i === 10 ? 10500 : 0 })),
  pipeline: [
    { statut: 'VISITE', nombre: 1, montantHt: 0 },
    { statut: 'ESTIMATION', nombre: 1, montantHt: 2000 },
    { statut: 'DOSSIER_TRAVAUX', nombre: 0, montantHt: 0 },
    { statut: 'CHANTIER', nombre: 1, montantHt: 10500 },
  ],
  aFaire: [{ type: 'AVENANT_EN_ATTENTE', libelle: 'Avenant n° 2 en attente de réponse', affaireId: 'a1',
    affaire: 'Maison gagnée', date: '2026-11-10T10:00:00Z' }],
}

const texte = (valeur: string | null) => (valeur ?? '').replace(/[  ]/g, ' ')

describe('TableauDeBordPage', () => {
  it('salue l’utilisateur et présente les indicateurs, l’agenda et les graphiques', async () => {
    const api = { tableauDeBord: vi.fn().mockResolvedValue(TABLEAU) }
    const me = { id: '1', subject: 's', email: null, displayName: 'Claire Martin', roles: [],
      organisation: { id: 'o', nom: 'Rénov Plus' }, roleEntreprise: 'GERANT' as const }
    render(
      <SessionContext.Provider value={{ api: api as unknown as Api, me }}>
        <MemoryRouter><TableauDeBordPage /></MemoryRouter>
      </SessionContext.Provider>,
    )

    expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent(/^(Bonjour|Bonsoir), Claire$/)
    expect(screen.getByText(/1 point\(s\) demandent votre attention/)).toBeInTheDocument()
    const signe = screen.getByRole('region', { name: 'Signé ce mois-ci' })
    expect(within(signe).getByText(/162,5 %/)).toBeInTheDocument()
    await vi.waitFor(() => expect(texte(within(signe).getByText(/€/).textContent)).toBe('10 500 €'), { timeout: 3000 })
    expect(screen.getByRole('link', { name: /Maison gagnée/ })).toHaveAttribute('href', '/affaires/a1')
    expect(screen.getByRole('img', { name: 'Montant signé par mois sur 12 mois' })).toBeInTheDocument()
    expect(texte(screen.getByRole('img', { name: /Montant des affaires par étape/ }).getAttribute('aria-label')))
      .toBe('Montant des affaires par étape, total 12 500,00 € HT')
  })
})
