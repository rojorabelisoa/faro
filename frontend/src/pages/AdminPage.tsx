import { useCallback, useEffect, useState } from 'react'
import type { OrganisationAdmin, Plan } from '../api'
import { useSession } from '../ApiContext'
import { joursRestants, LIBELLES_PLAN } from '../format'

const date = (valeur: string | null) => (valeur ? new Date(valeur).toLocaleDateString('fr-FR') : '—')

/** Administration du SaaS (rôle « admin ») : entreprises clientes, activité et offre. */
export default function AdminPage() {
  const { api } = useSession()
  const [organisations, setOrganisations] = useState<OrganisationAdmin[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  const run = useCallback(async (action: () => Promise<unknown>) => {
    try {
      await action()
      setOrganisations(await api.adminOrganisations())
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }, [api])

  useEffect(() => {
    void run(async () => undefined)
  }, [run])

  return (
    <>
      <div className="titre-page"><h1>Administration</h1></div>
      <p className="sous-titre">Entreprises clientes de faro. L'offre se change ici en attendant le paiement en ligne.</p>
      {error && <p role="alert" className="erreur">{error}</p>}
      {organisations && (
        <table className="tableau">
          <thead>
            <tr>
              <th>Entreprise</th>
              <th>Créée le</th>
              <th className="nombre">Utilisateurs</th>
              <th className="nombre">Affaires</th>
              <th>Dernière connexion</th>
              <th>Offre</th>
            </tr>
          </thead>
          <tbody>
            {organisations.map((o) => (
              <tr key={o.id}>
                <td>{o.nom}</td>
                <td>{date(o.createdAt)}</td>
                <td className="nombre">{o.utilisateurs}</td>
                <td className="nombre">{o.affaires}</td>
                <td>{date(o.derniereActivite)}</td>
                <td>
                  <select value={o.plan} aria-label={`Offre de ${o.nom}`}
                    onChange={(e) => void run(() => api.adminChangerPlan(o.id, e.target.value as Plan))}>
                    {Object.entries(LIBELLES_PLAN).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                  </select>
                  {o.plan === 'ESSAI' && o.essaiFin && (
                    <span className="discret"> {joursRestants(o.essaiFin)} j</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
