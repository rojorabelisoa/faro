import { useCallback, useEffect, useState, type FormEvent } from 'react'
import type { Lot, Marge, NatureDepense } from '../api'
import { useSession } from '../ApiContext'
import { formatEuros, parseNombre } from '../format'

const NATURES: Record<NatureDepense, string> = {
  MATERIAUX: 'Matériaux',
  MAIN_OEUVRE: "Main-d'œuvre",
  SOUS_TRAITANCE: 'Sous-traitance',
  LOCATION: 'Location',
  AUTRE: 'Autre',
}
const pourcent = (taux: number | null) => (taux == null ? '—' : `${String(taux).replace('.', ',')} %`)

/** Marge prévue (déboursé sec) contre marge réelle (dépenses saisies), lot par lot. */
export default function MargeChantier({ affaireId, lots, version }: { affaireId: string; lots: Lot[]; version: unknown }) {
  const { api } = useSession()
  const [marge, setMarge] = useState<Marge | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [lotId, setLotId] = useState('')
  const [nature, setNature] = useState<NatureDepense>('MATERIAUX')
  const [libelle, setLibelle] = useState('')
  const [fournisseur, setFournisseur] = useState('')
  const [montant, setMontant] = useState('')
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10))

  const run = useCallback(async (action: () => Promise<unknown>) => {
    try {
      await action()
      setMarge(await api.marge(affaireId))
      setError(null)
      return true
    } catch (e) {
      setError((e as Error).message)
      return false
    }
  }, [api, affaireId])

  useEffect(() => {
    void run(async () => undefined)
  }, [run, version])

  function ajouter(event: FormEvent) {
    event.preventDefault()
    const montantHt = parseNombre(montant)
    if (Number.isNaN(montantHt)) {
      setError('Le montant doit être un nombre.')
      return
    }
    void run(async () => {
      await api.addDepense(affaireId, { lotId, nature, libelle: libelle.trim(), fournisseur: fournisseur.trim() || null,
        montantHt, dateDepense: date })
      setLibelle('')
      setFournisseur('')
      setMontant('')
    })
  }

  const nomLot = (id: string) => {
    const lot = lots.find((l) => l.id === id)
    return lot ? `${lot.code} · ${lot.nom}` : ''
  }
  const classeMarge = (valeur: number) => (valeur < 0 ? 'negatif' : undefined)

  return (
    <section className="carte" aria-label="Marge">
      <h2>Marge</h2>
      {error && <p role="alert" className="erreur">{error}</p>}
      {marge && marge.lots.length > 0 && (
        <>
          <table className="tableau marge">
            <thead>
              <tr>
                <th>Lot</th>
                <th className="nombre">Vendu HT</th>
                <th className="nombre">Coût prévu</th>
                <th className="nombre">Coût réel</th>
                <th className="nombre">Marge prévue</th>
                <th className="nombre">Marge réelle</th>
              </tr>
            </thead>
            <tbody>
              {marge.lots.map((lot) => (
                <tr key={lot.lotId}>
                  <td>
                    {lot.code} · {lot.nom}
                    {lot.lignesSansDebourse > 0 && (
                      <div className="avertissement">{lot.lignesSansDebourse} ligne(s) sans déboursé sec</div>
                    )}
                  </td>
                  <td className="nombre">{formatEuros(lot.venduHt)}</td>
                  <td className="nombre">{formatEuros(lot.coutPrevuHt)}</td>
                  <td className="nombre">{formatEuros(lot.coutReelHt)}</td>
                  <td className={`nombre ${classeMarge(lot.margePrevueHt) ?? ''}`}>{formatEuros(lot.margePrevueHt)}</td>
                  <td className={`nombre ${classeMarge(lot.margeReelleHt) ?? ''}`}>{formatEuros(lot.margeReelleHt)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="total">
                <th>Total</th>
                <td className="nombre">{formatEuros(marge.total.venduHt)}</td>
                <td className="nombre">{formatEuros(marge.total.coutPrevuHt)}</td>
                <td className="nombre">{formatEuros(marge.total.coutReelHt)}</td>
                <td className="nombre">{formatEuros(marge.total.margePrevueHt)} ({pourcent(marge.total.tauxMargePrevue)})</td>
                <td className={`nombre ${classeMarge(marge.total.margeReelleHt) ?? ''}`}>
                  {formatEuros(marge.total.margeReelleHt)} ({pourcent(marge.total.tauxMargeReelle)})
                </td>
              </tr>
            </tfoot>
          </table>
          {marge.total.lignesSansDebourse > 0 && (
            <p className="discret">
              La marge prévue est surestimée : renseignez le déboursé sec des lignes signalées (ou dans la bibliothèque).
            </p>
          )}
        </>
      )}

      <h3>Dépenses réelles</h3>
      {marge && marge.depenses.length > 0 ? (
        <table className="tableau">
          <tbody>
            {marge.depenses.map((d) => (
              <tr key={d.id}>
                <td className="discret">{new Date(d.dateDepense).toLocaleDateString('fr-FR')}</td>
                <td>{d.libelle}{d.fournisseur && <span className="discret"> · {d.fournisseur}</span>}</td>
                <td className="discret">{nomLot(d.lotId)} · {NATURES[d.nature]}</td>
                <td className="nombre">{formatEuros(d.montantHt)}</td>
                <td className="actions-ligne">
                  <button className="danger" aria-label={`Supprimer ${d.libelle}`}
                    onClick={() => void run(() => api.deleteDepense(affaireId, d.id))}>Supprimer</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : <p className="vide">Saisissez les factures fournisseurs, la sous-traitance et les heures pour suivre la marge réelle.</p>}

      <form className="grille" onSubmit={ajouter} aria-label="Ajouter une dépense">
        <label className="large">Libellé<input value={libelle} onChange={(e) => setLibelle(e.target.value)} required
          placeholder="Facture Cedeo n° 1234, 2 jours plaquiste…" /></label>
        <label>Lot
          <select value={lotId} onChange={(e) => setLotId(e.target.value)} required>
            <option value="" disabled>Choisir…</option>
            {lots.map((l) => <option key={l.id} value={l.id}>{l.code} · {l.nom}</option>)}
          </select>
        </label>
        <label>Nature
          <select value={nature} onChange={(e) => setNature(e.target.value as NatureDepense)}>
            {Object.entries(NATURES).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label>
        <label>Fournisseur<input value={fournisseur} onChange={(e) => setFournisseur(e.target.value)} /></label>
        <label>Montant HT (€)<input value={montant} onChange={(e) => setMontant(e.target.value)} inputMode="decimal" required /></label>
        <label>Date<input type="date" value={date} onChange={(e) => setDate(e.target.value)} required /></label>
        <div className="actions pleine"><button type="submit">Ajouter la dépense</button></div>
      </form>
    </section>
  )
}
