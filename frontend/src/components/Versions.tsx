import { useCallback, useEffect, useState, type FormEvent } from 'react'
import type { Affaire, Version } from '../api'
import { useSession } from '../ApiContext'
import { formatEuros } from '../format'
import { nomFichier, telecharger } from './telecharger'

const dateHeure = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'short' })

/**
 * Versions figées du devis (ce qui a été envoyé au client) et passage en chantier :
 * le dossier travaux figé devient la référence du conducteur de travaux.
 */
export default function Versions({ affaire, onChantier }: { affaire: Affaire; onChantier: () => Promise<unknown> }) {
  const { api } = useSession()
  const [versions, setVersions] = useState<Version[] | null>(null)
  const [libelle, setLibelle] = useState('')
  const [error, setError] = useState<string | null>(null)

  const run = useCallback(async (action: () => Promise<unknown>) => {
    try {
      await action()
      setVersions(await api.versions(affaire.id))
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }, [api, affaire.id])

  useEffect(() => {
    void run(async () => undefined)
  }, [run])

  function figer(event: FormEvent) {
    event.preventDefault()
    void run(async () => {
      await api.figerVersion(affaire.id, libelle.trim())
      setLibelle('')
    })
  }

  function chantier() {
    if (!window.confirm('Figer le dossier travaux et passer l\'affaire en chantier ? Les changements suivants '
      + 'passeront par des avenants.')) return
    void run(async () => {
      await api.passerEnChantier(affaire.id)
      await onChantier()
    })
  }

  return (
    <section className="carte" aria-label="Versions et chantier">
      <div className="titre-section">
        <h2>Versions et chantier</h2>
        {!affaire.versionChantierId && (
          <button className="primaire" onClick={chantier}>Passer en chantier</button>
        )}
      </div>
      {error && <p role="alert" className="erreur">{error}</p>}
      {versions && versions.length > 0 ? (
        <table className="tableau">
          <thead>
            <tr>
              <th>Version</th>
              <th>Date</th>
              <th className="nombre">Total TTC</th>
              <th><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            {versions.map((version) => (
              <tr key={version.id}>
                <td>
                  {version.numero}. {version.libelle}
                  {version.chantier && <span className="statut statut-CHANTIER"> Référence chantier</span>}
                </td>
                <td className="discret">{dateHeure.format(new Date(version.createdAt))}{version.auteur && ` · ${version.auteur}`}</td>
                <td className="nombre">{formatEuros(version.totalTtc)}</td>
                <td className="actions-ligne">
                  {(['PIECE', 'LOT'] as const).map((vue) => (
                    <button key={vue} onClick={() => void run(async () => telecharger(
                      await api.versionPdf(affaire.id, version.id, vue),
                      `devis-${nomFichier(affaire.nom)}-v${version.numero}-${vue === 'PIECE' ? 'client' : 'chantier'}.pdf`))}>
                      PDF {vue === 'PIECE' ? 'client' : 'chantier'}
                    </button>
                  ))}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <p className="vide">Figez une version avant de l'envoyer au client : vous garderez une trace exacte de ce qui a été proposé.</p>
      )}
      <form className="en-ligne" onSubmit={figer} aria-label="Figer une version">
        <input value={libelle} onChange={(e) => setLibelle(e.target.value)} aria-label="Libellé de la version"
          placeholder="Devis envoyé au client…" />
        <button type="submit">Figer une version</button>
      </form>
    </section>
  )
}
