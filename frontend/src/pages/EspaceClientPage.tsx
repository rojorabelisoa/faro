import { useCallback, useEffect, useMemo, useState } from 'react'
import { createPublicApi, type Avenant, type EspaceClient, type Gamme } from '../api'
import { telecharger } from '../components/telecharger'
import { formatEuros, formatNombre, LIBELLES_GAMME } from '../format'

/**
 * Espace client (lien secret, sans compte) : le client consulte son devis pièce par pièce,
 * choisit ses gammes avant le chantier et valide ses avenants.
 */
export default function EspaceClientPage({ code }: { code: string }) {
  const api = useMemo(() => createPublicApi(code), [code])
  const [espace, setEspace] = useState<EspaceClient | null>(null)
  const [error, setError] = useState<string | null>(null)

  const run = useCallback(async (action: () => Promise<unknown>) => {
    try {
      await action()
      setEspace(await api.espace())
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }, [api])

  useEffect(() => {
    void run(async () => undefined)
  }, [run])

  if (!espace) {
    return <main className="centre">{error ? <p role="alert" className="erreur">{error}</p> : 'Chargement…'}</main>
  }

  const { devis } = espace
  const nomLot = (id: string) => espace.lots.find((l) => l.id === id)?.nom ?? ''

  return (
    <>
      <header className="entete">
        <span className="logo">{espace.entreprise}</span>
        <span className="utilisateur">Espace client</span>
      </header>
      <main className="contenu espace-client">
        <h1>{espace.affaire}</h1>
        {espace.adresse && <p className="sous-titre">{espace.adresse}</p>}
        {error && <p role="alert" className="erreur">{error}</p>}

        <section className="carte">
          <div className="titre-section">
            <h2>Votre devis</h2>
            <button onClick={() => void run(async () => telecharger(await api.devisPdf(), 'devis.pdf'))}>
              Télécharger le PDF
            </button>
          </div>
          {!espace.chantier && devis.totauxParGamme && (
            <p className="discret">Pour certaines pièces, plusieurs finitions vous sont proposées : choisissez la vôtre, le total se met à jour.</p>
          )}
          {devis.parPiece.filter((p) => devis.lignes.some((l) => l.pieceId === p.pieceId)).map((piece) => (
            <div key={piece.pieceId ?? 'general'} className="piece-client">
              <div className="titre-section">
                <h3>{piece.nom}</h3>
                <strong>{formatEuros(piece.totalHt)} HT</strong>
              </div>
              {piece.gammes && (
                <div className="choix-gammes" role="group" aria-label={`Finition ${piece.nom}`}>
                  {(Object.keys(LIBELLES_GAMME) as Gamme[]).map((gamme) => (
                    <button key={gamme} aria-pressed={piece.gammeChoisie === gamme} disabled={espace.chantier}
                      onClick={() => void run(() => api.choisirGamme(piece.pieceId, gamme))}>
                      <span>{LIBELLES_GAMME[gamme]}</span>
                      <strong>{formatEuros(piece.gammes![gamme])} HT</strong>
                    </button>
                  ))}
                </div>
              )}
              <ul className="lignes-client">
                {devis.lignes.filter((l) => l.pieceId === piece.pieceId && l.incluse).map((ligne) => (
                  <li key={ligne.id}>
                    <span>
                      {ligne.designation}
                      <span className="discret"> · {nomLot(ligne.lotId)} · {formatNombre(ligne.quantite)} {ligne.unite}</span>
                      {ligne.description && <span className="discret descriptif">{ligne.description}</span>}
                    </span>
                    <span className="nombre">{formatEuros(ligne.montantHt)}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
          <dl className="marche">
            <div><dt>Total HT</dt><dd>{formatEuros(devis.totalHt)}</dd></div>
            <div><dt>TVA</dt><dd>{formatEuros(devis.totalTva)}</dd></div>
            <div className="total"><dt>Total TTC</dt><dd>{formatEuros(devis.totalTtc)}</dd></div>
          </dl>
        </section>

        {espace.chantier && (
          <section className="carte">
            <h2>Travaux en cours</h2>
            <dl className="marche">
              <div><dt>Marché initial</dt><dd>{formatEuros(espace.marche.initialTtc)} TTC</dd></div>
              <div><dt>Avenants acceptés</dt><dd>{formatEuros(espace.marche.avenantsValidesTtc)} TTC</dd></div>
              <div className="total"><dt>Montant actuel</dt><dd>{formatEuros(espace.marche.actuelTtc)} TTC</dd></div>
            </dl>
            {espace.avenants.length === 0 && <p className="vide">Aucun avenant pour l'instant.</p>}
            {espace.avenants.map((avenant) => (
              <AvenantClient key={avenant.id} avenant={avenant} nomLot={nomLot}
                onPdf={() => void run(async () => telecharger(await api.avenantPdf(avenant.id), `avenant-${avenant.numero}.pdf`))}
                onDecision={(decision, nom, commentaire) => void run(() => api.decider(avenant.id, { decision, nom, commentaire }))} />
            ))}
          </section>
        )}
      </main>
    </>
  )
}

function AvenantClient({ avenant, nomLot, onPdf, onDecision }: {
  avenant: Avenant
  nomLot: (id: string) => string
  onPdf: () => void
  onDecision: (decision: 'VALIDE' | 'REFUSE', nom: string, commentaire: string | null) => void
}) {
  const [nom, setNom] = useState('')
  const [commentaire, setCommentaire] = useState('')
  return (
    <article className="avenant" aria-label={`Avenant ${avenant.numero}`}>
      <div className="titre-section">
        <h3>Avenant n° {avenant.numero} — {avenant.objet}</h3>
        <strong>{formatEuros(avenant.totaux.totalTtc)} TTC</strong>
      </div>
      {avenant.motif && <p className="discret">{avenant.motif}</p>}
      <ul className="lignes-client">
        {avenant.lignes.map((l) => (
          <li key={l.id}>
            <span>{l.designation}<span className="discret"> · {nomLot(l.lotId)} · {formatNombre(l.quantite)} {l.unite}</span></span>
            <span className="nombre">{formatEuros(l.montantHt)}</span>
          </li>
        ))}
      </ul>
      <button onClick={onPdf}>Télécharger le PDF</button>
      {avenant.statut === 'ENVOYE' ? (
        <form className="decision-client" aria-label={`Votre réponse à l'avenant ${avenant.numero}`}
          onSubmit={(e) => e.preventDefault()}>
          <label>Votre nom<input value={nom} onChange={(e) => setNom(e.target.value)} required /></label>
          <label>Commentaire (facultatif)<input value={commentaire} onChange={(e) => setCommentaire(e.target.value)} /></label>
          <div className="actions">
            <button type="button" className="danger" disabled={!nom.trim()}
              onClick={() => onDecision('REFUSE', nom.trim(), commentaire.trim() || null)}>Je refuse</button>
            <button type="button" className="primaire" disabled={!nom.trim()}
              onClick={() => onDecision('VALIDE', nom.trim(), commentaire.trim() || null)}>J'accepte cet avenant</button>
          </div>
        </form>
      ) : (
        <p className={avenant.statut === 'VALIDE' ? 'info' : 'avertissement'}>
          {avenant.statut === 'VALIDE' ? 'Accepté' : 'Refusé'}
          {avenant.decideAt && ` le ${new Date(avenant.decideAt).toLocaleDateString('fr-FR')}`}
          {avenant.decidePar && ` par ${avenant.decidePar}`}
        </p>
      )}
    </article>
  )
}
