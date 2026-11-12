import { motion, type Variants } from 'motion/react'
import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import type { Indicateur, TableauDeBord } from '../api'
import { useSession } from '../ApiContext'
import { Anneau, Compteur, MiniCourbe } from '../components/tableau/graphiques'
import Pipeline from '../components/tableau/Pipeline'
import SigneParMois from '../components/tableau/SigneParMois'
import { formatEuros } from '../format'

const apparition: Variants = {
  cache: { opacity: 0, y: 16 },
  visible: (i: number) => ({ opacity: 1, y: 0, transition: { delay: i * 0.06, duration: 0.5, ease: [0.16, 1, 0.3, 1] } }),
}
const euros = (n: number) => formatEuros(Math.round(n)).replace(',00', '')
const entier = (n: number) => String(Math.round(n))
const pourcent = (n: number) => `${n.toFixed(1).replace('.', ',')} %`

function salutation(maintenant: Date) {
  return maintenant.getHours() >= 18 ? 'Bonsoir' : 'Bonjour'
}

/** Page d'accueil : l'activité de l'entreprise en un coup d'œil (style « verre dépoli »). */
export default function TableauDeBordPage() {
  const { api, me } = useSession()
  const [tableau, setTableau] = useState<TableauDeBord | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.tableauDeBord().then(setTableau).catch((e: Error) => setError(e.message))
  }, [api])

  const maintenant = new Date()
  const prenom = (me.displayName ?? '').split(' ')[0]
  const dateLongue = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }).format(maintenant)

  return (
    <div className="tdb">
      <header className="tdb-entete">
        <div>
          <h1>{salutation(maintenant)}{prenom && `, ${prenom}`}</h1>
          <p className="tdb-sous-titre">
            {dateLongue.charAt(0).toUpperCase() + dateLongue.slice(1)}
            {tableau && ` · ${tableau.aFaire.length === 0 ? 'rien d’urgent' : `${tableau.aFaire.length} point(s) demandent votre attention`}`}
          </p>
        </div>
        <div className="tdb-actions">
          <Link className="tdb-bouton" to="/entreprise">Inviter un collègue</Link>
          <Link className="tdb-bouton sombre" to="/affaires?nouvelle">+ Nouvelle affaire</Link>
        </div>
      </header>

      {error && <p role="alert" className="erreur">{error}</p>}
      {!tableau && !error && <p className="tdb-sous-titre">Chargement…</p>}

      {tableau && (
        <motion.div className="tdb-grille" initial="cache" animate="visible">
          <Hero tableau={tableau} />
          <Agenda tableau={tableau} maintenant={maintenant} />

          <Carte i={2} titre="Affaires actives" indicateur={tableau.affairesActives} format={entier} icone={<IconeDossier />} />
          <Carte i={3} titre="Signé ce mois-ci" indicateur={tableau.signeCeMois} format={euros} icone={<IconeEuro />} accent />
          <Carte i={4} titre="Marge réelle" indicateur={tableau.margeReelle} format={pourcent} icone={<IconeCourbe />} />
          <Carte i={5} titre="En attente client" indicateur={tableau.enAttenteClient} format={euros} icone={<IconeSablier />} />

          <motion.section className="verre tdb-large" custom={6} variants={apparition}>
            <h2>Montant signé par mois</h2>
            <SigneParMois donnees={tableau.signeParMois} />
          </motion.section>
          <motion.section className="verre tdb-etroit" custom={7} variants={apparition}>
            <h2>Affaires par étape</h2>
            <Pipeline pipeline={tableau.pipeline} />
          </motion.section>
        </motion.div>
      )}
    </div>
  )
}

function Hero({ tableau }: { tableau: TableauDeBord }) {
  const { transformation, signeCeMois, pipeline } = tableau
  const enChantier = pipeline.find((e) => e.statut === 'CHANTIER')?.nombre ?? 0
  const lance = (signeCeMois.valeur ?? 0) > 0
  return (
    <motion.section className="verre-corail tdb-hero" custom={0} variants={apparition}>
      <div className="tdb-hero-texte">
        <span className="tdb-badge">Ce mois-ci · {enChantier} chantier(s) en cours</span>
        <h2>
          {lance ? <>Votre carnet est <em>bien rempli</em> ce mois-ci</> : <>Prêt pour de <em>nouveaux</em> chantiers</>}
        </h2>
        <p>
          {lance ? `${euros(signeCeMois.valeur ?? 0)} HT signés depuis le 1er du mois.`
            : 'Aucun dossier signé ce mois-ci pour l’instant : les affaires en estimation sont vos prochaines signatures.'}
        </p>
        <dl className="tdb-hero-chiffres">
          <div><dt>affaires gagnées</dt><dd><Compteur valeur={transformation.gagnees} format={entier} /></dd></div>
          <div><dt>en négociation</dt><dd><Compteur valeur={transformation.enCours} format={entier} /></dd></div>
          <div><dt>perdues</dt><dd><Compteur valeur={transformation.perdues} format={entier} /></dd></div>
        </dl>
        <div className="tdb-hero-actions">
          <Link className="tdb-bouton clair" to="/affaires">Voir les affaires ↗</Link>
        </div>
      </div>
      <div className="tdb-anneau">
        <Anneau pourcent={transformation.taux} libelle="Taux de transformation" />
        <div className="tdb-anneau-valeur">
          <strong>{transformation.taux == null ? '—' : <Compteur valeur={transformation.taux} format={(n) => `${Math.round(n)} %`} />}</strong>
          <span>transformées</span>
        </div>
      </div>
    </motion.section>
  )
}

function Agenda({ tableau, maintenant }: { tableau: TableauDeBord; maintenant: Date }) {
  const lundi = new Date(maintenant)
  lundi.setDate(maintenant.getDate() - ((maintenant.getDay() + 6) % 7))
  const jours = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(lundi)
    d.setDate(lundi.getDate() + i)
    return d
  })
  const jourCourt = new Intl.DateTimeFormat('fr-FR', { weekday: 'narrow' })
  return (
    <motion.section className="verre tdb-agenda" custom={1} variants={apparition}>
      <div className="tdb-agenda-date">
        <strong>{String(maintenant.getDate()).padStart(2, '0')}</strong>
        <div>
          <span>{new Intl.DateTimeFormat('fr-FR', { weekday: 'long' }).format(maintenant)}</span>
          <span className="discret">{new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(maintenant)}</span>
        </div>
      </div>
      <ol className="tdb-semaine" aria-label="Semaine en cours">
        {jours.map((d) => (
          <li key={d.toISOString()} className={d.toDateString() === maintenant.toDateString() ? 'aujourdhui' : undefined}>
            <span>{jourCourt.format(d)}</span>{d.getDate()}
          </li>
        ))}
      </ol>
      <h3>À faire</h3>
      {tableau.aFaire.length === 0 ? <p className="discret">Rien d'urgent. 👌</p> : (
        <ul className="tdb-a-faire">
          {tableau.aFaire.slice(0, 4).map((item) => (
            <li key={`${item.type}-${item.affaireId}-${item.libelle}`} className={`type-${item.type}`}>
              <Link to={`/affaires/${item.affaireId}`}>
                <strong>{item.affaire}</strong>
                <span>{item.libelle}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </motion.section>
  )
}

function Carte({ i, titre, indicateur, format, icone, accent = false }: {
  i: number
  titre: string
  indicateur: Indicateur
  format: (n: number) => string
  icone: ReactNode
  accent?: boolean
}) {
  return (
    <motion.section className={`${accent ? 'verre-corail' : 'verre'} tdb-carte`} custom={i} variants={apparition}
      whileHover={{ y: -5 }} aria-label={titre}>
      <div className="tdb-carte-haut">
        <span className="tdb-icone">{icone}</span>
        {indicateur.tendance != null && (
          <span className={`tdb-tendance ${indicateur.tendance >= 0 ? 'hausse' : 'baisse'}`}>
            {indicateur.tendance >= 0 ? '▲' : '▼'} {Math.abs(indicateur.tendance).toFixed(1).replace('.', ',')} %
          </span>
        )}
      </div>
      <div className="tdb-valeur">
        {indicateur.valeur == null ? '—' : <Compteur valeur={indicateur.valeur} format={format} />}
      </div>
      <div className="tdb-carte-bas">
        <div>
          <h2>{titre}</h2>
          <p>{indicateur.detail}</p>
        </div>
        <MiniCourbe serie={indicateur.serie} couleur={accent ? '#fff' : undefined} />
      </div>
    </motion.section>
  )
}

const Icone = ({ children }: { children: ReactNode }) => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
    strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{children}</svg>
)
const IconeDossier = () => <Icone><path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" /></Icone>
const IconeEuro = () => <Icone><path d="M18 7a7 7 0 1 0 0 10M4 10h10M4 14h10" /></Icone>
const IconeCourbe = () => <Icone><path d="M3 17l6-6 4 4 8-8M15 7h6v6" /></Icone>
const IconeSablier = () => <Icone><path d="M6 3h12M6 21h12M7 3c0 5 10 5 10 9s-10 4-10 9M17 3c0 5-10 5-10 9s10 4 10 9" /></Icone>
