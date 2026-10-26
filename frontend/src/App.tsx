import { useEffect, useMemo, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { BrowserRouter, NavLink, Route, Routes } from 'react-router'
import { createApi, type Me } from './api'
import { SessionContext, useSession } from './ApiContext'
import AffairesPage from './pages/AffairesPage'
import AffairePage from './pages/AffairePage'
import BibliothequePage from './pages/BibliothequePage'

export default function App() {
  const auth = useAuth()
  const accessToken = auth.user?.access_token
  const api = useMemo(() => (accessToken ? createApi(accessToken) : null), [accessToken])
  const [me, setMe] = useState<Me | null>(null)
  const [error, setError] = useState<string | null>(null)

  // /api/me en premier : il crée l'utilisateur et son organisation à la première connexion.
  useEffect(() => {
    if (!api) {
      setMe(null)
      return
    }
    api.me()
      .then((data) => {
        setMe(data)
        setError(null)
      })
      .catch((e: Error) => setError(e.message))
  }, [api])

  if (auth.isLoading) {
    return <main className="centre">Chargement…</main>
  }

  if (auth.error) {
    return <main className="centre" role="alert">Erreur de connexion : {auth.error.message}</main>
  }

  if (!auth.isAuthenticated) {
    return (
      <main className="centre">
        <h1>faro</h1>
        <p>Le pilotage de vos chantiers de rénovation.</p>
        <button className="primaire" onClick={() => void auth.signinRedirect()}>Se connecter</button>
      </main>
    )
  }

  if (error) {
    return <main className="centre" role="alert">{error}</main>
  }

  if (!api || !me) {
    return <main className="centre">Chargement du profil…</main>
  }

  return (
    <SessionContext.Provider value={{ api, me }}>
      <BrowserRouter>
        <Layout onLogout={() => void auth.signoutRedirect()}>
          <Routes>
            <Route path="/" element={<AffairesPage />} />
            <Route path="/affaires/:id" element={<AffairePage />} />
            <Route path="/bibliotheque" element={<BibliothequePage />} />
          </Routes>
        </Layout>
      </BrowserRouter>
    </SessionContext.Provider>
  )
}

function Layout({ children, onLogout }: { children: React.ReactNode; onLogout: () => void }) {
  const { me } = useSession()
  return (
    <>
      <header className="entete">
        <span className="logo">faro</span>
        <nav>
          <NavLink to="/" end>Affaires</NavLink>
          <NavLink to="/bibliotheque">Bibliothèque</NavLink>
        </nav>
        <span className="utilisateur">
          {me.displayName ?? me.email ?? me.subject} · {me.organisation.nom}
        </span>
        <button onClick={onLogout}>Se déconnecter</button>
      </header>
      <main className="contenu">{children}</main>
    </>
  )
}
