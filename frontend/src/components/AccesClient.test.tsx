import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Api, LienClient } from '../api'
import { SessionContext } from '../ApiContext'
import AccesClient from './AccesClient'

const lienClient = (id: string, expireAt: string, expire: boolean): LienClient => ({
  id, code: `code-${id}`, createdAt: '2026-08-01T10:00:00Z', derniereVisite: null, expireAt, expire,
})

describe('AccesClient', () => {
  it('affiche la date d’expiration des liens et prolonge un lien expiré', async () => {
    const valide = lienClient('1', '2027-02-12T10:00:00Z', false)
    const expire = lienClient('2', '2026-11-01T10:00:00Z', true)
    const api = {
      liensClient: vi.fn().mockResolvedValueOnce([valide, expire])
        .mockResolvedValue([valide, { ...expire, expireAt: '2027-02-12T12:00:00Z', expire: false }]),
      prolongerLienClient: vi.fn().mockResolvedValue({}),
    }
    const me = { id: '1', subject: 's', email: null, displayName: null, roles: [], organisation: { id: 'o', nom: 'X' },
      roleEntreprise: 'GERANT' as const }
    render(
      <SessionContext.Provider value={{ api: api as unknown as Api, me }}>
        <AccesClient affaireId="a1" />
      </SessionContext.Provider>,
    )

    expect(await screen.findByText(/valable jusqu'au 12 février 2027/)).toBeInTheDocument()
    const ligneExpiree = screen.getByText(/expiré depuis le 1 novembre 2026/).closest('li')!
    expect(within(ligneExpiree).queryByRole('button', { name: 'Copier' })).not.toBeInTheDocument()

    await userEvent.click(within(ligneExpiree).getByRole('button', { name: 'Prolonger' }))
    expect(api.prolongerLienClient).toHaveBeenCalledWith('a1', '2')
    expect(await screen.findAllByText(/valable jusqu'au 12 février 2027/)).toHaveLength(2)
    expect(screen.queryByText(/expiré depuis/)).not.toBeInTheDocument()
  })
})
