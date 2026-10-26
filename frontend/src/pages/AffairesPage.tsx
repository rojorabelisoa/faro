import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import type { Affaire, TypeBien } from '../api'
import { useSession } from '../ApiContext'
import { LIBELLES_STATUT, LIBELLES_TYPE_BIEN } from '../format'

export default function AffairesPage() {
  const { api } = useSession()
  const navigate = useNavigate()
  const [affaires, setAffaires] = useState<Affaire[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [creation, setCreation] = useState(false)

  useEffect(() => {
    api.affaires().then(setAffaires).catch((e: Error) => setError(e.message))
  }, [api])

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      const affaire = await api.createAffaire({
        nom: String(form.get('nom')),
        clientNom: String(form.get('clientNom')) || null,
        adresse: String(form.get('adresse')) || null,
        typeBien: form.get('typeBien') as TypeBien,
      })
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
        <form className="carte formulaire" onSubmit={create} aria-label="Nouvelle affaire">
          <label>
            Nom de l'affaire
            <input name="nom" required placeholder="Rénovation T3 rue Victor Hugo" />
          </label>
          <label>
            Client
            <input name="clientNom" placeholder="M. et Mme Martin" />
          </label>
          <label>
            Adresse
            <input name="adresse" />
          </label>
          <label>
            Type de bien
            <select name="typeBien" defaultValue="APPARTEMENT">
              {Object.entries(LIBELLES_TYPE_BIEN).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>
          <div className="actions">
            <button type="button" onClick={() => setCreation(false)}>Annuler</button>
            <button type="submit" className="primaire">Créer</button>
          </div>
        </form>
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
