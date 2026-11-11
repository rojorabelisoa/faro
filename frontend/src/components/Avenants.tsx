import { useCallback, useEffect, useState, type FormEvent } from 'react'
import type { Affaire, Avenant, Lot, Marche, Piece } from '../api'
import { useSession } from '../ApiContext'
import { formatEuros, formatNombre, formatTaux, parseNombre, TAUX_TVA, UNITES } from '../format'
import { nomFichier, telecharger } from './telecharger'

const LIBELLES_STATUT_AVENANT: Record<string, string> = {
  BROUILLON: 'Brouillon',
  ENVOYE: 'Envoyé au client',
  VALIDE: 'Validé',
  REFUSE: 'Refusé',
}
const dateHeure = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'short' })

/** Travaux supplémentaires (ou en moins) pendant le chantier, et montant du marché qui en résulte. */
export default function Avenants({ affaire, pieces, lots }: { affaire: Affaire; pieces: Piece[]; lots: Lot[] }) {
  const { api } = useSession()
  const [avenants, setAvenants] = useState<Avenant[] | null>(null)
  const [marche, setMarche] = useState<Marche | null>(null)
  const [objet, setObjet] = useState('')
  const [motif, setMotif] = useState('')
  const [error, setError] = useState<string | null>(null)

  const run = useCallback(async (action: () => Promise<unknown>) => {
    try {
      await action()
      const [a, m] = await Promise.all([api.avenants(affaire.id), api.marche(affaire.id)])
      setAvenants(a)
      setMarche(m)
      setError(null)
      return true
    } catch (e) {
      setError((e as Error).message)
      return false
    }
  }, [api, affaire.id])

  useEffect(() => {
    void run(async () => undefined)
  }, [run])

  function creer(event: FormEvent) {
    event.preventDefault()
    void run(async () => {
      await api.createAvenant(affaire.id, { objet: objet.trim(), motif: motif.trim() || null })
      setObjet('')
      setMotif('')
    })
  }

  return (
    <section className="carte" aria-label="Avenants">
      <h2>Avenants et montant du marché</h2>
      {error && <p role="alert" className="erreur">{error}</p>}
      {marche && (
        <dl className="marche">
          <div><dt>Marché initial</dt><dd>{formatEuros(marche.initialTtc)} TTC</dd></div>
          <div><dt>Avenants validés</dt><dd>{formatEuros(marche.avenantsValidesTtc)} TTC</dd></div>
          <div className="total"><dt>Marché actuel</dt><dd>{formatEuros(marche.actuelTtc)} TTC</dd></div>
          {marche.avenantsEnAttenteHt !== 0 && (
            <div><dt>En attente de réponse</dt><dd>{formatEuros(marche.avenantsEnAttenteHt)} HT</dd></div>
          )}
        </dl>
      )}

      {avenants?.map((avenant) => (
        <CarteAvenant key={avenant.id} affaire={affaire} avenant={avenant} pieces={pieces} lots={lots} run={run} />
      ))}

      <form className="formulaire-ligne" onSubmit={creer} aria-label="Nouvel avenant">
        <h3>Nouvel avenant</h3>
        <div className="grille">
          <label className="large">
            Objet
            <input value={objet} onChange={(e) => setObjet(e.target.value)} required
              placeholder="Prises supplémentaires, changement de carrelage…" />
          </label>
          <label className="pleine">
            Motif (facultatif)
            <textarea value={motif} onChange={(e) => setMotif(e.target.value)} rows={2}
              placeholder="Demande du client, découverte en cours de chantier…" />
          </label>
        </div>
        <div className="actions"><button type="submit" className="primaire">Créer l'avenant</button></div>
      </form>
    </section>
  )
}

function CarteAvenant({ affaire, avenant, pieces, lots, run }: {
  affaire: Affaire
  avenant: Avenant
  pieces: Piece[]
  lots: Lot[]
  run: (action: () => Promise<unknown>) => Promise<boolean>
}) {
  const { api } = useSession()
  const [par, setPar] = useState(affaire.clientNom ?? '')
  const [commentaire, setCommentaire] = useState('')
  const nomPiece = (id: string | null) => pieces.find((p) => p.id === id)?.nom ?? 'Général'
  const nomLot = (id: string) => {
    const lot = lots.find((l) => l.id === id)
    return lot ? `${lot.code} · ${lot.nom}` : ''
  }

  return (
    <article className="avenant" aria-label={`Avenant ${avenant.numero}`}>
      <div className="titre-section">
        <h3>
          Avenant n° {avenant.numero} — {avenant.objet}{' '}
          <span className={`statut statut-avenant-${avenant.statut}`}>{LIBELLES_STATUT_AVENANT[avenant.statut]}</span>
        </h3>
        <span className="nombre">{formatEuros(avenant.totaux.totalTtc)} TTC</span>
      </div>
      {avenant.motif && <p className="discret">{avenant.motif}</p>}
      {avenant.lignes.length > 0 && (
        <table className="tableau">
          <tbody>
            {avenant.lignes.map((ligne) => (
              <tr key={ligne.id}>
                <td>{ligne.designation}</td>
                <td className="discret">{nomPiece(ligne.pieceId)} · {nomLot(ligne.lotId)}</td>
                <td className="nombre">{formatNombre(ligne.quantite)} {ligne.unite}</td>
                <td className="nombre">{formatEuros(ligne.prixUnitaireHt)}</td>
                <td className="nombre">{formatEuros(ligne.montantHt)}</td>
                <td className="actions-ligne">
                  {avenant.statut === 'BROUILLON' && (
                    <button className="danger" aria-label={`Supprimer ${ligne.designation}`}
                      onClick={() => void run(() => api.deleteLigne(affaire.id, ligne.id))}>Supprimer</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {avenant.statut === 'BROUILLON' && (
        <>
          <LigneAvenant pieces={pieces} lots={lots}
            onSubmit={(input) => run(() => api.addLigneAvenant(affaire.id, avenant.id, input))} />
          <div className="actions">
            <button className="danger" onClick={() => void run(() => api.deleteAvenant(affaire.id, avenant.id))}>
              Supprimer l'avenant
            </button>
            <button className="primaire" disabled={avenant.lignes.length === 0}
              onClick={() => void run(() => api.envoyerAvenant(affaire.id, avenant.id))}>
              Envoyer au client (fige le contenu)
            </button>
          </div>
        </>
      )}

      {avenant.statut !== 'BROUILLON' && (
        <div className="en-ligne">
          <button onClick={() => void run(async () => telecharger(await api.avenantPdf(affaire.id, avenant.id),
            `avenant-${avenant.numero}-${nomFichier(affaire.nom)}.pdf`))}>PDF de l'avenant</button>
          {avenant.envoyeAt && <span className="discret">Envoyé le {dateHeure.format(new Date(avenant.envoyeAt))}</span>}
        </div>
      )}

      {avenant.statut === 'ENVOYE' && (
        <form className="en-ligne decision" aria-label={`Décision avenant ${avenant.numero}`}
          onSubmit={(e) => e.preventDefault()}>
          <input value={par} onChange={(e) => setPar(e.target.value)} placeholder="Nom du client" required
            aria-label="Décision prise par" />
          <input value={commentaire} onChange={(e) => setCommentaire(e.target.value)} placeholder="Commentaire"
            aria-label="Commentaire de la décision" />
          {(['VALIDE', 'REFUSE'] as const).map((decision) => (
            <button key={decision} type="button" className={decision === 'VALIDE' ? 'primaire' : 'danger'}
              disabled={!par.trim()}
              onClick={() => void run(() => api.deciderAvenant(affaire.id, avenant.id,
                { decision, par: par.trim(), commentaire: commentaire.trim() || null }))}>
              {decision === 'VALIDE' ? 'Validé par le client' : 'Refusé par le client'}
            </button>
          ))}
        </form>
      )}

      {avenant.decideAt && (
        <p className={avenant.statut === 'VALIDE' ? 'info' : 'avertissement'}>
          {LIBELLES_STATUT_AVENANT[avenant.statut]} le {dateHeure.format(new Date(avenant.decideAt))} par {avenant.decidePar}
          {avenant.decisionCommentaire && ` — « ${avenant.decisionCommentaire} »`}
        </p>
      )}
    </article>
  )
}

function LigneAvenant({ pieces, lots, onSubmit }: {
  pieces: Piece[]
  lots: Lot[]
  onSubmit: (input: import('../api').LigneInput) => Promise<boolean>
}) {
  const [pieceId, setPieceId] = useState('')
  const [lotId, setLotId] = useState('')
  const [designation, setDesignation] = useState('')
  const [unite, setUnite] = useState('U')
  const [quantite, setQuantite] = useState('')
  const [prix, setPrix] = useState('')
  const [tauxTva, setTauxTva] = useState(10)
  const [invalide, setInvalide] = useState<string | null>(null)

  async function submit(event: FormEvent) {
    event.preventDefault()
    const q = parseNombre(quantite)
    const pu = parseNombre(prix)
    if (Number.isNaN(q) || q < 0 || Number.isNaN(pu)) {
      setInvalide('Quantité et prix doivent être des nombres (prix négatif pour une moins-value).')
      return
    }
    setInvalide(null)
    if (await onSubmit({ pieceId: pieceId || null, lotId, designation: designation.trim(), unite, quantite: q,
      prixUnitaireHt: pu, tauxTva })) {
      setDesignation('')
      setQuantite('')
      setPrix('')
    }
  }

  return (
    <form className="grille ligne-avenant" onSubmit={submit} aria-label="Ajouter une ligne à l'avenant">
      <label className="large">Désignation<input value={designation} onChange={(e) => setDesignation(e.target.value)} required /></label>
      <label>Pièce
        <select value={pieceId} onChange={(e) => setPieceId(e.target.value)}>
          <option value="">Général</option>
          {pieces.map((p) => <option key={p.id} value={p.id}>{p.nom}</option>)}
        </select>
      </label>
      <label>Lot
        <select value={lotId} onChange={(e) => setLotId(e.target.value)} required>
          <option value="" disabled>Choisir…</option>
          {lots.map((l) => <option key={l.id} value={l.id}>{l.code} · {l.nom}</option>)}
        </select>
      </label>
      <label>Quantité<input value={quantite} onChange={(e) => setQuantite(e.target.value)} inputMode="decimal" required /></label>
      <label>Unité
        <select value={unite} onChange={(e) => setUnite(e.target.value)}>{UNITES.map((u) => <option key={u}>{u}</option>)}</select>
      </label>
      <label>PU HT (€)<input value={prix} onChange={(e) => setPrix(e.target.value)} inputMode="decimal" required /></label>
      <label>TVA
        <select value={tauxTva} onChange={(e) => setTauxTva(Number(e.target.value))}>
          {TAUX_TVA.map((t) => <option key={t} value={t}>{formatTaux(t)}</option>)}
        </select>
      </label>
      <div className="actions pleine">
        {invalide && <span role="alert" className="erreur">{invalide}</span>}
        <button type="submit">Ajouter la ligne</button>
      </div>
    </form>
  )
}
