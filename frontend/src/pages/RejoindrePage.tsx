import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { useSession } from '../ApiContext'
import { LIBELLES_ROLE } from '../format'

/** Page ouverte depuis un lien d'invitation : rejoindre l'entreprise d'un collègue. */
export default function RejoindrePage() {
  const { code = '' } = useParams()
  const { api, me } = useSession()
  const [info, setInfo] = useState<{ entreprise: string; role: string; utilisable: boolean } | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.invitationInfo(code).then(setInfo).catch((e: Error) => setError(e.message))
  }, [api, code])

  async function rejoindre() {
    try {
      await api.accepterInvitation(code)
      // Recharge l'application : la session (entreprise, rôle) a changé.
      window.location.assign('/')
    } catch (e) {
      setError((e as Error).message)
    }
  }

  if (error) return <p role="alert" className="erreur">{error}</p>
  if (!info) return <p>Chargement…</p>
  if (!info.utilisable) return <p className="erreur">Cette invitation a expiré ou a déjà été utilisée. Demandez un nouveau lien.</p>

  return (
    <section className="carte centre-carte">
      <h1>Rejoindre {info.entreprise}</h1>
      <p>Vous êtes invité(e) comme <strong>{LIBELLES_ROLE[info.role]}</strong>.</p>
      {me.organisation.nom !== info.entreprise && (
        <p className="discret">Vous quitterez « {me.organisation.nom} » (supprimée si elle est vide).</p>
      )}
      <button className="primaire" onClick={() => void rejoindre()}>Rejoindre l'entreprise</button>
    </section>
  )
}
