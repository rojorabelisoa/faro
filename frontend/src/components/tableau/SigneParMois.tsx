import { useState } from 'react'
import { motion } from 'motion/react'
import { formatEuros } from '../../format'

const moisCourt = new Intl.DateTimeFormat('fr-FR', { month: 'short' })
const moisLong = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' })
const versDate = (mois: string) => new Date(`${mois}-15T12:00:00`)

/**
 * Montant signé par mois (une série) : barres fines arrondies au sommet, ancrées sur l'axe,
 * séparées par un espace ; info-bulle au survol, table équivalente pour les lecteurs d'écran.
 */
export default function SigneParMois({ donnees }: { donnees: { mois: string; montantHt: number }[] }) {
  const [survol, setSurvol] = useState<number | null>(null)
  const largeur = 640
  const hauteur = 200
  const marge = { haut: 12, bas: 26, gauche: 8, droite: 8 }
  const max = Math.max(1, ...donnees.map((d) => d.montantHt))
  const pas = (largeur - marge.gauche - marge.droite) / donnees.length
  const barre = Math.max(6, Math.min(28, pas - 10))
  const y = (v: number) => hauteur - marge.bas - (v / max) * (hauteur - marge.haut - marge.bas)
  const total = donnees.reduce((s, d) => s + d.montantHt, 0)

  return (
    <div className="graphique-signe">
      <svg viewBox={`0 0 ${largeur} ${hauteur}`} role="img" aria-label="Montant signé par mois sur 12 mois"
        onMouseLeave={() => setSurvol(null)}>
        <line x1={marge.gauche} x2={largeur - marge.droite} y1={hauteur - marge.bas} y2={hauteur - marge.bas}
          className="axe" />
        {donnees.map((d, i) => {
          const x = marge.gauche + i * pas + (pas - barre) / 2
          const h = hauteur - marge.bas - y(d.montantHt)
          const rayon = Math.min(4, h / 2)
          return (
            <g key={d.mois} onMouseEnter={() => setSurvol(i)}>
              {/* zone de survol plus large que la barre */}
              <rect x={marge.gauche + i * pas} y={marge.haut} width={pas} height={hauteur - marge.haut - marge.bas}
                fill="transparent" />
              {d.montantHt > 0 && (
                <motion.path
                  d={`M${x},${hauteur - marge.bas} v${-(h - rayon)} q0,${-rayon} ${rayon},${-rayon} h${barre - 2 * rayon} q${rayon},0 ${rayon},${rayon} v${h - rayon} z`}
                  className={survol === i ? 'barre active' : 'barre'}
                  initial={{ opacity: 0, scaleY: 0 }} animate={{ opacity: 1, scaleY: 1 }}
                  style={{ transformOrigin: `0 ${hauteur - marge.bas}px` }}
                  transition={{ duration: 0.6, delay: 0.04 * i, ease: [0.16, 1, 0.3, 1] }} />
              )}
              <text x={marge.gauche + i * pas + pas / 2} y={hauteur - 8} textAnchor="middle" className="etiquette-axe">
                {moisCourt.format(versDate(d.mois)).replace('.', '')}
              </text>
            </g>
          )
        })}
        {survol != null && (
          <line x1={marge.gauche + survol * pas + pas / 2} x2={marge.gauche + survol * pas + pas / 2}
            y1={marge.haut} y2={hauteur - marge.bas} className="reticule" />
        )}
      </svg>
      {survol != null && (
        <div className="info-bulle" style={{ left: `${((marge.gauche + survol * pas + pas / 2) / largeur) * 100}%` }}>
          <span>{moisLong.format(versDate(donnees[survol].mois))}</span>
          <strong>{formatEuros(donnees[survol].montantHt)} HT</strong>
        </div>
      )}
      <p className="discret">Total sur 12 mois : <strong>{formatEuros(total)} HT</strong></p>
      <div className="sr-only"><table>
        <caption>Montant signé par mois</caption>
        <tbody>
          {donnees.map((d) => (
            <tr key={d.mois}><th>{moisLong.format(versDate(d.mois))}</th><td>{formatEuros(d.montantHt)}</td></tr>
          ))}
        </tbody>
      </table></div>
    </div>
  )
}
