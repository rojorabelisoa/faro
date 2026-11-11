import { useCallback, useEffect, useState } from 'react'
import type { Ratio } from '../api'
import { useSession } from '../ApiContext'
import { formatEuros, LIBELLES_NIVEAU, LIBELLES_TYPE_PIECE, parseNombre } from '../format'

/** Ratios €/m² HT de l'estimation rapide, avec les valeurs constatées dans les devis pour les recaler. */
export default function RatiosPage() {
  const { api } = useSession()
  const [ratios, setRatios] = useState<Ratio[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)

  const load = useCallback(() => api.ratios().then(setRatios).catch((e: Error) => setError(e.message)), [api])
  useEffect(() => {
    void load()
  }, [load])

  async function enregistrer(ratio: Ratio, minSaisi: string, maxSaisi: string) {
    const min = parseNombre(minSaisi)
    const max = parseNombre(maxSaisi)
    if (Number.isNaN(min) || Number.isNaN(max)) {
      setError('Les prix au m² doivent être des nombres.')
      return
    }
    try {
      await api.updateRatio(ratio.id, { prixMinM2: min, prixMaxM2: max })
      setError(null)
      setInfo(`${LIBELLES_TYPE_PIECE[ratio.typePiece]} – ${LIBELLES_NIVEAU[ratio.niveauTravaux]} enregistré.`)
      await load()
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <>
      <div className="titre-page">
        <h1>Ratios d'estimation</h1>
      </div>
      <p className="sous-titre">
        Prix HT au m² utilisés pour l'estimation rapide. Les valeurs de départ sont indicatives : remplacez-les par
        les vôtres. La colonne « Constaté » montre ce que coûtent réellement vos pièces dans vos devis.
      </p>
      {error && <p role="alert" className="erreur">{error}</p>}
      {info && <p role="status" className="info" onClick={() => setInfo(null)}>{info}</p>}
      {ratios === null ? <p>Chargement…</p> : (
        <table className="tableau">
          <thead>
            <tr>
              <th>Pièce</th>
              <th>Niveau</th>
              <th className="nombre">Min €/m²</th>
              <th className="nombre">Max €/m²</th>
              <th className="nombre">Constaté (médiane)</th>
              <th><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            {ratios.map((ratio) => <LigneRatio key={`${ratio.id}-${ratio.prixMinM2}-${ratio.prixMaxM2}`} ratio={ratio}
              onSave={enregistrer} />)}
          </tbody>
        </table>
      )}
    </>
  )
}

function LigneRatio({ ratio, onSave }: { ratio: Ratio; onSave: (ratio: Ratio, min: string, max: string) => void }) {
  const [min, setMin] = useState(String(ratio.prixMinM2).replace('.', ','))
  const [max, setMax] = useState(String(ratio.prixMaxM2).replace('.', ','))
  const libelle = `${LIBELLES_TYPE_PIECE[ratio.typePiece]} ${LIBELLES_NIVEAU[ratio.niveauTravaux]}`
  const modifie = min !== String(ratio.prixMinM2).replace('.', ',') || max !== String(ratio.prixMaxM2).replace('.', ',')
  return (
    <tr>
      <td>{LIBELLES_TYPE_PIECE[ratio.typePiece]}</td>
      <td>{LIBELLES_NIVEAU[ratio.niveauTravaux]}</td>
      <td className="nombre">
        <input value={min} onChange={(e) => setMin(e.target.value)} inputMode="decimal" className="tres-court"
          aria-label={`Minimum ${libelle}`} />
      </td>
      <td className="nombre">
        <input value={max} onChange={(e) => setMax(e.target.value)} inputMode="decimal" className="tres-court"
          aria-label={`Maximum ${libelle}`} />
      </td>
      <td className="nombre">
        {ratio.constate ? (
          <span title={`${ratio.constate.pieces} pièce(s), de ${formatEuros(ratio.constate.minM2)} à ${formatEuros(ratio.constate.maxM2)}`}>
            {formatEuros(ratio.constate.medianeM2)} <span className="discret">({ratio.constate.pieces})</span>
          </span>
        ) : <span className="discret">—</span>}
      </td>
      <td className="actions-ligne">
        <button disabled={!modifie} onClick={() => onSave(ratio, min, max)} aria-label={`Enregistrer ${libelle}`}>
          Enregistrer
        </button>
      </td>
    </tr>
  )
}
