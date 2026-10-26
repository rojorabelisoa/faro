import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuth } from 'react-oidc-context'
import App from './App'

vi.mock('react-oidc-context', () => ({ useAuth: vi.fn() }))

const mockedUseAuth = vi.mocked(useAuth)

function mockAuth(overrides: Record<string, unknown>) {
  const auth = {
    isLoading: false,
    isAuthenticated: false,
    error: undefined,
    user: null,
    signinRedirect: vi.fn(),
    signoutRedirect: vi.fn(),
    ...overrides,
  }
  mockedUseAuth.mockReturnValue(auth as unknown as ReturnType<typeof useAuth>)
  return auth
}

const ME = {
  id: '1', subject: 's', email: 'demo@faro.local', displayName: 'Demo Faro', roles: ['user'],
  organisation: { id: 'o', nom: 'Rénov Plus' },
}

/** Répond selon l'URL appelée, comme le ferait l'API. */
function mockApi(routes: Record<string, unknown>) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const path = new URL(String(input)).pathname
    if (!(path in routes)) return new Response('', { status: 404 })
    const body = routes[path]
    return typeof body === 'number' ? new Response('', { status: body }) : new Response(JSON.stringify(body))
  })
}

describe('App', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    window.history.pushState({}, '', '/')
  })

  it('propose de se connecter quand personne ne l’est', async () => {
    const auth = mockAuth({})
    render(<App />)

    await userEvent.click(screen.getByRole('button', { name: 'Se connecter' }))

    expect(auth.signinRedirect).toHaveBeenCalled()
  })

  it('affiche l’utilisateur, son entreprise et ses affaires', async () => {
    mockAuth({ isAuthenticated: true, user: { access_token: 'jeton-123' } })
    const fetchSpy = mockApi({
      '/api/me': ME,
      '/api/affaires': [{
        id: 'a1', nom: 'T3 Victor Hugo', clientNom: 'M. Martin', typeBien: 'APPARTEMENT', statut: 'VISITE',
      }],
    })

    render(<App />)

    expect(await screen.findByText('Demo Faro · Rénov Plus')).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'T3 Victor Hugo' })).toHaveAttribute('href', '/affaires/a1')
    expect(screen.getByText('Visite')).toBeInTheDocument()
    expect(fetchSpy.mock.calls[0][0]).toBe('http://localhost:8080/api/me')
    expect(fetchSpy.mock.calls[0][1]).toMatchObject({ headers: { Authorization: 'Bearer jeton-123' } })
  })

  it('affiche une erreur si l’API refuse le jeton', async () => {
    mockAuth({ isAuthenticated: true, user: { access_token: 'mauvais' } })
    mockApi({ '/api/me': 401 })

    render(<App />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Erreur API 401')
  })
})
