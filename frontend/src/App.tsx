import { useEffect, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { fetchMe, type Me } from './api'

export default function App() {
  const auth = useAuth()
  const [me, setMe] = useState<Me | null>(null)
  const [error, setError] = useState<string | null>(null)
  const accessToken = auth.user?.access_token

  useEffect(() => {
    if (!accessToken) {
      setMe(null)
      return
    }
    fetchMe(accessToken)
      .then((data) => {
        setMe(data)
        setError(null)
      })
      .catch((e: Error) => setError(e.message))
  }, [accessToken])

  if (auth.isLoading) {
    return <main>Chargement…</main>
  }

  if (auth.error) {
    return <main role="alert">Erreur de connexion : {auth.error.message}</main>
  }

  if (!auth.isAuthenticated) {
    return (
      <main>
        <h1>faro</h1>
        <button onClick={() => void auth.signinRedirect()}>Se connecter</button>
      </main>
    )
  }

  return (
    <main>
      <h1>faro</h1>
      {error && <p role="alert">{error}</p>}
      {me ? (
        <section>
          <p>Bonjour {me.displayName ?? me.email ?? me.subject}</p>
          <p>Rôles : {me.roles.join(', ') || 'aucun'}</p>
        </section>
      ) : (
        !error && <p>Chargement du profil…</p>
      )}
      <button onClick={() => void auth.signoutRedirect()}>Se déconnecter</button>
    </main>
  )
}
