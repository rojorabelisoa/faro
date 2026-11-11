import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import type { Estimation as EstimationData } from '../api'
import { useSession } from '../ApiContext'
import { formatEuros, LIBELLES_NIVEAU, LIBELLES_TYPE_PIECE } from '../format'

/**
 * Estimation rapide en fourchette (surface × ratios €/m²), à donner au client avant le devis détaillé.
 * `version` change à chaque rechargement de l'affaire : l'estimation est alors recalculée.
 */
export default function Estimation({ affaireId, version }: { affaireId: string; version: unknown }) {
  const { api } = useSession()
  const [estimation, setEstimation] = useState<EstimationData | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.estimation(affaireId).then(setEstimation).catch((e: Error) => setError(e.message))
  }, [api, affaireId, version])

  if (error) return <p role="alert" className="erreur">{error}</p>
  if (!estimation || estimation.pieces.length === 0) return null

  const estimees = estimation.pieces.filter((p) => p.manque == null)
  const devisDansFourchette = estimation.devisHt >= estimation.totalMinHt && estimation.devisHt <= estimation.totalMaxHt

  return (
    <section className="carte" aria-label="Estimation rapide">
      <div className="titre-section">
        <h2>Estimation rapide</h2>
        <Link to="/ratios">Ajuster les ratios €/m²</Link>
      </div>
      {estimees.length > 0 ? (
        <p className="fourchette">
          {formatEuros(estimation.totalMinHt)} – {formatEuros(estimation.totalMaxHt)} <span className="discret">HT</span>
        </p>
      ) : (
        <p className="discret">Indiquez la surface, le type et le niveau de travaux des pièces pour obtenir une fourchette.</p>
      )}
      <ul className="estimation-pieces">
        {estimation.pieces.map((piece) => (
          <li key={piece.pieceId}>
            <span>
              {piece.nom}
              {piece.typePiece && piece.niveauTravaux && (
                <span className="discret"> · {LIBELLES_TYPE_PIECE[piece.typePiece]}, {LIBELLES_NIVEAU[piece.niveauTravaux].toLowerCase()}</span>
              )}
            </span>
            {piece.manque ? (
              <span className="avertissement">{piece.manque} manquant{piece.manque === 'surface' ? 'e' : ''}</span>
            ) : (
              <span className="nombre">{formatEuros(piece.minHt ?? 0)} – {formatEuros(piece.maxHt ?? 0)}</span>
            )}
          </li>
        ))}
      </ul>
      {estimation.devisHt > 0 && estimees.length > 0 && (
        <p className={devisDansFourchette ? 'discret' : 'avertissement'}>
          Devis détaillé : {formatEuros(estimation.devisHt)} HT,{' '}
          {devisDansFourchette ? 'dans la fourchette.' : 'hors de la fourchette : vérifiez les oublis ou les ratios.'}
        </p>
      )}
    </section>
  )
}
