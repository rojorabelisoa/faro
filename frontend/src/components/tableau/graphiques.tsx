import { animate, motion, useInView } from 'motion/react'
import { useEffect, useRef, useState } from 'react'

/** Nombre qui défile de 0 à sa valeur à l'apparition. */
export function Compteur({ valeur, format }: { valeur: number; format: (n: number) => string }) {
  const [affiche, setAffiche] = useState(0)
  const ref = useRef<HTMLSpanElement>(null)
  const visible = useInView(ref, { once: true })
  useEffect(() => {
    if (!visible) return
    const controle = animate(0, valeur, { duration: 1.1, ease: [0.16, 1, 0.3, 1], onUpdate: setAffiche })
    return () => controle.stop()
  }, [valeur, visible])
  return <span ref={ref}>{format(affiche)}</span>
}

/** Anneau de progression (0–100 %), tracé animé. */
export function Anneau({ pourcent, taille = 132, libelle }: { pourcent: number | null; taille?: number; libelle: string }) {
  const rayon = taille / 2 - 9
  const circonference = 2 * Math.PI * rayon
  const valeur = Math.max(0, Math.min(100, pourcent ?? 0))
  return (
    <svg width={taille} height={taille} viewBox={`0 0 ${taille} ${taille}`} role="img"
      aria-label={`${libelle} : ${pourcent == null ? 'pas encore de données' : `${pourcent} %`}`}>
      <circle cx={taille / 2} cy={taille / 2} r={rayon} fill="none" stroke="rgba(255,255,255,0.28)" strokeWidth="9" />
      <motion.circle cx={taille / 2} cy={taille / 2} r={rayon} fill="none" stroke="#fff" strokeWidth="9"
        strokeLinecap="round" strokeDasharray={circonference} transform={`rotate(-90 ${taille / 2} ${taille / 2})`}
        initial={{ strokeDashoffset: circonference }}
        animate={{ strokeDashoffset: circonference * (1 - valeur / 100) }}
        transition={{ duration: 1.4, ease: [0.16, 1, 0.3, 1], delay: 0.2 }} />
    </svg>
  )
}

/** Mini-courbe d'une série (une seule série : pas de légende, la carte la nomme). */
export function MiniCourbe({ serie, couleur = 'var(--tdb-accent)' }: { serie: number[]; couleur?: string }) {
  if (serie.length < 2) return null
  const largeur = 96
  const hauteur = 32
  const max = Math.max(...serie)
  const min = Math.min(...serie)
  const etendue = max - min || 1
  const points = serie.map((v, i) => [
    (i / (serie.length - 1)) * (largeur - 4) + 2,
    hauteur - 3 - ((v - min) / etendue) * (hauteur - 6),
  ])
  const d = points.map(([x, y], i) => `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`).join(' ')
  return (
    <svg width={largeur} height={hauteur} viewBox={`0 0 ${largeur} ${hauteur}`} aria-hidden="true" className="mini-courbe">
      <motion.path d={d} fill="none" stroke={couleur} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
        initial={{ pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ duration: 1.2, delay: 0.3 }} />
    </svg>
  )
}
