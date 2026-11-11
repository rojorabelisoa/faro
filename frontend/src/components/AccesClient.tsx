import { useCallback, useEffect, useState } from 'react'
import type { LienClient } from '../api'
import { useSession } from '../ApiContext'

const lien = (code: string) => `${window.location.origin}/client/${code}`

/** Liens secrets de l'espace client de l'affaire (consultation, choix des gammes, validation des avenants). */
export default function AccesClient({ affaireId }: { affaireId: string }) {
  const { api } = useSession()
  const [liens, setLiens] = useState<LienClient[]>([])
  const [info, setInfo] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const run = useCallback(async (action: () => Promise<unknown>) => {
    try {
      await action()
      setLiens(await api.liensClient(affaireId))
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }, [api, affaireId])

  useEffect(() => {
    void run(async () => undefined)
  }, [run])

  async function copier(code: string) {
    try {
      await navigator.clipboard.writeText(lien(code))
      setInfo('Lien copié : envoyez-le à votre client (e-mail, SMS).')
    } catch {
      setInfo(lien(code))
    }
  }

  return (
    <section className="carte" aria-label="Espace client">
      <div className="titre-section">
        <h2>Espace client</h2>
        <button onClick={() => void run(() => api.creerLienClient(affaireId))}>Créer un lien client</button>
      </div>
      <p className="discret">
        Le client consulte son devis, choisit ses finitions et valide ses avenants, sans créer de compte.
        Toute personne ayant le lien y a accès : révoquez-le s'il a circulé.
      </p>
      {error && <p role="alert" className="erreur">{error}</p>}
      {info && <p role="status" className="info" onClick={() => setInfo(null)}>{info}</p>}
      <ul className="estimation-pieces">
        {liens.map((l) => (
          <li key={l.id}>
            <span>
              <code className="lien-invitation">{lien(l.code)}</code>
              <span className="discret"> · {l.derniereVisite
                ? `dernière visite le ${new Date(l.derniereVisite).toLocaleString('fr-FR')}` : 'pas encore ouvert'}</span>
            </span>
            <span>
              <button onClick={() => void copier(l.code)}>Copier</button>{' '}
              <button className="danger" onClick={() => void run(() => api.revoquerLienClient(affaireId, l.id))}>Révoquer</button>
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}
