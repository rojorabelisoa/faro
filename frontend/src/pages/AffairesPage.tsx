import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import type { Affaire, AffaireInput } from '../api'
import FormulaireAffaire from '../components/FormulaireAffaire'
import { useSession } from '../ApiContext'
import { LIBELLES_STATUT, LIBELLES_TYPE_BIEN } from '../format'

export default function AffairesPage() {
  const { api } = useSession()
  const navigate = useNavigate()
  const [affaires, setAffaires] = useState<Affaire[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [creation, setCreation] = useState(() => new URLSearchParams(window.location.search).has('nouvelle'))

  useEffect(() => {
    api.affaires().then(setAffaires).catch((e: Error) => setError(e.message))
  }, [api])

  async function create(input: AffaireInput) {
    try {
      const affaire = await api.createAffaire(input)
      navigate(`/affaires/${affaire.id}`)
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <>
      <div className="titre-page">
        <h1>Affaires</h1>
        {!creation && <button className="primaire" onClick={() => setCreation(true)}>Nouvelle affaire</button>}
      </div>

      {error && <p role="alert" className="erreur">{error}</p>}

      {creation && (
        <FormulaireAffaire libelle="Nouvelle affaire" onSubmit={create} onCancel={() => setCreation(false)} />
      )}

      {affaires === null ? (
        !error && <p>Chargement…</p>
      ) : affaires.length === 0 ? (
        <p className="vide">Aucune affaire pour l'instant. Créez la première après une visite.</p>
      ) : (
        <table className="tableau">
          <thead>
            <tr>
              <th>Affaire</th>
              <th>Client</th>
              <th>Type</th>
              <th>Statut</th>
            </tr>
          </thead>
          <tbody>
            {affaires.map((affaire) => (
              <tr key={affaire.id}>
                <td><Link to={`/affaires/${affaire.id}`}>{affaire.nom}</Link></td>
                <td>{affaire.clientNom}</td>
                <td>{LIBELLES_TYPE_BIEN[affaire.typeBien]}</td>
                <td><span className={`statut statut-${affaire.statut}`}>{LIBELLES_STATUT[affaire.statut]}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
