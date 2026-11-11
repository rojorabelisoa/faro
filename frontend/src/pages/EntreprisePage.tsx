import { useCallback, useEffect, useState, type FormEvent } from 'react'
import type { Entreprise, Invitation, RoleEntreprise } from '../api'
import { useSession } from '../ApiContext'
import { LIBELLES_ROLE } from '../format'

const lienInvitation = (code: string) => `${window.location.origin}/rejoindre/${code}`

/** L'entreprise : son nom, son équipe et les invitations (gestion réservée au gérant). */
export default function EntreprisePage() {
  const { api, me } = useSession()
  const gerant = me.roleEntreprise === 'GERANT'
  const [entreprise, setEntreprise] = useState<Entreprise | null>(null)
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [nom, setNom] = useState('')
  const [role, setRole] = useState<RoleEntreprise>('CHARGE_ETUDES')
  const [email, setEmail] = useState('')
  const [nouvelle, setNouvelle] = useState<Invitation | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)

  const run = useCallback(async (action: () => Promise<unknown>) => {
    try {
      await action()
      const e = await api.entreprise()
      setEntreprise(e)
      setNom(e.nom)
      if (gerant) setInvitations(await api.invitations())
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }, [api, gerant])

  useEffect(() => {
    void run(async () => undefined)
  }, [run])

  function renommer(event: FormEvent) {
    event.preventDefault()
    void run(async () => {
      await api.renommerEntreprise(nom.trim())
      setInfo('Nom enregistré. Il apparaîtra sur vos devis.')
    })
  }

  function inviter(event: FormEvent) {
    event.preventDefault()
    void run(async () => {
      setNouvelle(await api.inviter(role, email.trim() || null))
      setEmail('')
    })
  }

  async function copier(code: string) {
    try {
      await navigator.clipboard.writeText(lienInvitation(code))
      setInfo('Lien copié : envoyez-le à votre collègue.')
    } catch {
      setInfo(lienInvitation(code))
    }
  }

  if (!entreprise) return error ? <p role="alert" className="erreur">{error}</p> : <p>Chargement…</p>

  return (
    <>
      <div className="titre-page"><h1>Entreprise</h1></div>
      {error && <p role="alert" className="erreur">{error}</p>}
      {info && <p role="status" className="info" onClick={() => setInfo(null)}>{info}</p>}

      <section className="carte">
        <h2>Nom de l'entreprise</h2>
        {gerant ? (
          <form className="en-ligne" onSubmit={renommer} aria-label="Renommer l'entreprise">
            <input value={nom} onChange={(e) => setNom(e.target.value)} required aria-label="Nom de l'entreprise" />
            <button type="submit">Enregistrer</button>
          </form>
        ) : <p>{entreprise.nom}</p>}
      </section>

      <section className="carte">
        <h2>Équipe</h2>
        <table className="tableau">
          <tbody>
            {entreprise.membres.map((membre) => (
              <tr key={membre.id}>
                <td>{membre.nom ?? membre.email}{membre.id === me.id && <span className="discret"> (vous)</span>}</td>
                <td className="discret">{membre.email}</td>
                <td>
                  {gerant ? (
                    <select value={membre.role} aria-label={`Rôle de ${membre.nom ?? membre.email}`}
                      onChange={(e) => void run(() => api.changerRole(membre.id, e.target.value as RoleEntreprise))}>
                      {Object.entries(LIBELLES_ROLE).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                    </select>
                  ) : LIBELLES_ROLE[membre.role]}
                </td>
                <td className="actions-ligne">
                  {gerant && membre.id !== me.id && (
                    <button className="danger" onClick={() => void run(() => api.retirerMembre(membre.id))}>Retirer</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {gerant && (
        <section className="carte">
          <h2>Inviter un collègue</h2>
          <form className="en-ligne" onSubmit={inviter} aria-label="Inviter un collègue">
            <select value={role} onChange={(e) => setRole(e.target.value as RoleEntreprise)} aria-label="Rôle de l'invité">
              {Object.entries(LIBELLES_ROLE).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </select>
            <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" placeholder="E-mail (pour mémoire)"
              aria-label="E-mail de l'invité" />
            <button type="submit" className="primaire">Créer un lien d'invitation</button>
          </form>
          {nouvelle && (
            <p className="info">
              Lien à envoyer (valable 7 jours, une seule utilisation) :{' '}
              <code className="lien-invitation">{lienInvitation(nouvelle.code)}</code>{' '}
              <button onClick={() => void copier(nouvelle.code)}>Copier</button>
            </p>
          )}
          {invitations.length > 0 && (
            <>
              <h3>Invitations en attente</h3>
              <ul className="estimation-pieces">
                {invitations.map((invitation) => (
                  <li key={invitation.id}>
                    <span>
                      {LIBELLES_ROLE[invitation.role]}{invitation.email && ` · ${invitation.email}`}
                      <span className="discret"> · expire le {new Date(invitation.expiresAt).toLocaleDateString('fr-FR')}</span>
                    </span>
                    <span>
                      <button onClick={() => void copier(invitation.code)}>Copier le lien</button>{' '}
                      <button className="danger" onClick={() => void run(() => api.revoquerInvitation(invitation.id))}>Révoquer</button>
                    </span>
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
      )}
    </>
  )
}
