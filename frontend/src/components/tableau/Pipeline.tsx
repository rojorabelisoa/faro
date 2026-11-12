import { useState } from 'react'
import { motion } from 'motion/react'
import type { TableauDeBord } from '../../api'
import { formatEuros, LIBELLES_STATUT } from '../../format'

/**
 * Montant par étape (ordonnée) : anneau en rampe séquentielle d'une seule teinte, segments séparés,
 * total au centre ; légende avec valeurs (l'identité ne repose jamais sur la seule couleur).
 */
export default function Pipeline({ pipeline }: { pipeline: TableauDeBord['pipeline'] }) {
  const [survol, setSurvol] = useState<number | null>(null)
  const total = pipeline.reduce((s, e) => s + e.montantHt, 0)
  const taille = 168
  const rayon = 64
  const epaisseur = 18
  const circonference = 2 * Math.PI * rayon
  const espace = total > 0 && pipeline.filter((e) => e.montantHt > 0).length > 1 ? 3 : 0
  let cumul = 0

  return (
    <div className="pipeline">
      <svg width={taille} height={taille} viewBox={`0 0 ${taille} ${taille}`} role="img"
        aria-label={`Montant des affaires par étape, total ${formatEuros(total)} HT`}>
        <circle cx={taille / 2} cy={taille / 2} r={rayon} fill="none" className="piste" strokeWidth={epaisseur} />
        {total > 0 && pipeline.map((etape, i) => {
          const longueur = (etape.montantHt / total) * circonference
          const decalage = cumul
          cumul += longueur
          if (longueur <= 0) return null
          return (
            <motion.circle key={etape.statut} cx={taille / 2} cy={taille / 2} r={rayon} fill="none"
              className={`segment etape-${i} ${survol === i ? 'active' : ''}`} strokeWidth={epaisseur}
              strokeDasharray={`${Math.max(0, longueur - espace)} ${circonference}`}
              strokeDashoffset={-decalage} transform={`rotate(-90 ${taille / 2} ${taille / 2})`}
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.15 * i + 0.2 }}
              onMouseEnter={() => setSurvol(i)} onMouseLeave={() => setSurvol(null)} />
          )
        })}
        <text x="50%" y="47%" textAnchor="middle" className="total-anneau">
          {formatEuros(survol != null ? pipeline[survol].montantHt : total).replace(',00', '')}
        </text>
        <text x="50%" y="60%" textAnchor="middle" className="sous-total-anneau">
          {survol != null ? LIBELLES_STATUT[pipeline[survol].statut] : 'en cours, HT'}
        </text>
      </svg>
      <ul className="legende">
        {pipeline.map((etape, i) => (
          <li key={etape.statut} onMouseEnter={() => setSurvol(i)} onMouseLeave={() => setSurvol(null)}>
            <span className={`pastille-couleur etape-${i}`} aria-hidden="true" />
            <span>{LIBELLES_STATUT[etape.statut]} <span className="discret">({etape.nombre})</span></span>
            <strong>{formatEuros(etape.montantHt)}</strong>
          </li>
        ))}
      </ul>
    </div>
  )
}
