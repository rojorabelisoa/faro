import type { AuthProviderProps } from 'react-oidc-context'
import { WebStorageStateStore } from 'oidc-client-ts'

const env = import.meta.env

export const apiUrl: string = env.VITE_API_URL ?? 'http://localhost:8080'

/** Connexion OIDC : Authorization Code + PKCE (activé par défaut dans oidc-client-ts). */
export const oidcConfig: AuthProviderProps = {
  // Non défini : Keycloak local de développement. Vide : même adresse que la page (mode réseau local, /auth).
  authority: env.VITE_OIDC_AUTHORITY === undefined ? 'http://localhost:8180/realms/faro'
    : env.VITE_OIDC_AUTHORITY || `${window.location.origin}/auth/realms/faro`,
  client_id: env.VITE_OIDC_CLIENT_ID ?? 'faro-frontend',
  redirect_uri: window.location.origin + '/',
  post_logout_redirect_uri: window.location.origin + '/',
  scope: 'openid profile email',
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  // Retire ?code=...&state=... de l'URL une fois la connexion terminée.
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname)
  },
}
