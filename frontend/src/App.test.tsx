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

describe('App', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('propose de se connecter quand personne ne l’est', async () => {
    const auth = mockAuth({})
    render(<App />)

    await userEvent.click(screen.getByRole('button', { name: 'Se connecter' }))

    expect(auth.signinRedirect).toHaveBeenCalled()
  })

  it('affiche l’utilisateur renvoyé par /api/me avec le jeton', async () => {
    mockAuth({ isAuthenticated: true, user: { access_token: 'jeton-123' } })
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({ id: '1', subject: 's', email: 'demo@faro.local', displayName: 'Demo Faro', roles: ['user'] }),
        { status: 200 },
      ),
    )

    render(<App />)

    expect(await screen.findByText('Bonjour Demo Faro')).toBeInTheDocument()
    expect(screen.getByText('Rôles : user')).toBeInTheDocument()
    expect(fetchSpy).toHaveBeenCalledWith('http://localhost:8080/api/me', {
      headers: { Authorization: 'Bearer jeton-123' },
    })
  })

  it('affiche une erreur si l’API refuse le jeton', async () => {
    mockAuth({ isAuthenticated: true, user: { access_token: 'mauvais' } })
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 401 }))

    render(<App />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Erreur API 401')
  })
})
