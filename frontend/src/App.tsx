import { useEffect, useMemo, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { BrowserRouter, NavLink, Route, Routes, useLocation } from 'react-router'
import { createApi, type Me } from './api'
import { joursRestants, LIBELLES_ROLE } from './format'
import {
  IconeAccueil, IconeAdmin, IconeAffaires, IconeBibliotheque, IconeEntreprise, IconeLots, IconeMenu, IconeRatios,
  IconeSortie,
} from './components/Icones'
import { SessionContext, useSession } from './ApiContext'
import AffairesPage from './pages/AffairesPage'
import TableauDeBordPage from './pages/TableauDeBordPage'
import AffairePage from './pages/AffairePage'
import BibliothequePage from './pages/BibliothequePage'
import LotsPage from './pages/LotsPage'
import EspaceClientPage from './pages/EspaceClientPage'
import RatiosPage from './pages/RatiosPage'
import EntreprisePage from './pages/EntreprisePage'
import AdminPage from './pages/AdminPage'
import RejoindrePage from './pages/RejoindrePage'

export default function App() {
  // L'espace client est public : pas de connexion, le code du lien suffit.
  const espaceClient = window.location.pathname.match(/^\/client\/([^/]+)/)
  return espaceClient ? <EspaceClientPage code={espaceClient[1]} /> : <Application />
}

function Application() {
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
    // Session périmée (ex. Keycloak réinitialisé) : on l'efface et on repart de la page de connexion.
    return (
      <main className="centre" role="alert">
        <p>Votre session a expiré ou n'est plus valide ({auth.error.message}).</p>
        <button className="primaire" onClick={() => void auth.removeUser().then(() => auth.signinRedirect())}>
          Se reconnecter
        </button>
      </main>
    )
  }

  if (!auth.isAuthenticated) {
    return (
      <main className="centre">
        <h1 className="logo">faro</h1>
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
            <Route path="/" element={<TableauDeBordPage />} />
            <Route path="/affaires" element={<AffairesPage />} />
            <Route path="/affaires/:id" element={<AffairePage />} />
            <Route path="/bibliotheque" element={<BibliothequePage />} />
            <Route path="/lots" element={<LotsPage />} />
            <Route path="/ratios" element={<RatiosPage />} />
            <Route path="/entreprise" element={<EntreprisePage />} />
            <Route path="/rejoindre/:code" element={<RejoindrePage />} />
            {me.roles.includes('admin') && <Route path="/admin" element={<AdminPage />} />}
          </Routes>
        </Layout>
      </BrowserRouter>
    </SessionContext.Provider>
  )
}

/** Rappelle la fin de l'essai gratuit (dans les 10 derniers jours). */
function BandeauEssai() {
  const { api } = useSession()
  const [essaiFin, setEssaiFin] = useState<string | null>(null)
  useEffect(() => {
    api.entreprise().then((e) => setEssaiFin(e.offre.essaiFin)).catch(() => setEssaiFin(null))
  }, [api])
  if (!essaiFin) return null
  const jours = joursRestants(essaiFin)
  if (jours > 10) return null
  return (
    <p className="bandeau-essai" role="status">
      {jours === 0 ? "Votre essai gratuit est terminé" : `Votre essai gratuit se termine dans ${jours} jour(s)`}
      {' — '}<NavLink to="/entreprise">voir les offres</NavLink>
    </p>
  )
}

const NAVIGATION = [
  { to: '/', libelle: 'Tableau de bord', icone: <IconeAccueil />, fin: true },
  { to: '/affaires', libelle: 'Affaires', icone: <IconeAffaires /> },
  { to: '/bibliotheque', libelle: 'Bibliothèque', icone: <IconeBibliotheque /> },
]
const REGLAGES = [
  { to: '/lots', libelle: 'Lots', icone: <IconeLots /> },
  { to: '/ratios', libelle: 'Ratios', icone: <IconeRatios /> },
  { to: '/entreprise', libelle: 'Entreprise', icone: <IconeEntreprise /> },
]

const initiales = (nom: string) => nom.split(/\s+/).filter(Boolean).slice(0, 2).map((m) => m[0].toUpperCase()).join('')

/** Barre latérale « verre dépoli » (tiroir sur mobile) et zone principale. */
function Layout({ children, onLogout }: { children: React.ReactNode; onLogout: () => void }) {
  const { me } = useSession()
  const [menuOuvert, setMenuOuvert] = useState(false)
  const location = useLocation()
  useEffect(() => setMenuOuvert(false), [location.pathname])
  const nom = me.displayName ?? me.email ?? me.subject
  const lien = (item: { to: string; libelle: string; icone: React.ReactNode; fin?: boolean }) => (
    <NavLink key={item.to} to={item.to} end={item.fin}>{item.icone}{item.libelle}</NavLink>
  )

  return (
    <div className={`app ${menuOuvert ? 'menu-ouvert' : ''}`}>
      <aside className="barre-laterale" aria-label="Navigation">
        <div className="marque">
          <span className="marque-logo" aria-hidden="true">f</span>
          <div><strong>faro</strong><span>{me.organisation.nom}</span></div>
        </div>
        <nav aria-label="Principale">
          <div className="section-nav">Pilotage</div>
          {NAVIGATION.map(lien)}
          <div className="section-nav">Réglages</div>
          {REGLAGES.map(lien)}
          {me.roles.includes('admin') && lien({ to: '/admin', libelle: 'Admin SaaS', icone: <IconeAdmin /> })}
        </nav>
        <div className="profil">
          <span className="avatar" aria-hidden="true">{initiales(nom)}</span>
          <div className="profil-texte">
            <strong>{nom}</strong>
            <span>{LIBELLES_ROLE[me.roleEntreprise]}</span>
          </div>
          <button onClick={onLogout} aria-label="Se déconnecter" title="Se déconnecter"><IconeSortie /></button>
        </div>
      </aside>
      <div className="voile" onClick={() => setMenuOuvert(false)} />
      <div className="principal">
        <header className="barre-mobile">
          <button onClick={() => setMenuOuvert(true)} aria-label="Ouvrir le menu" aria-expanded={menuOuvert}><IconeMenu /></button>
          <span className="logo">faro</span>
        </header>
        <BandeauEssai />
        <main className="contenu">{children}</main>
      </div>
    </div>
  )
}
