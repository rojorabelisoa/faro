import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import type {
  Affaire, Devis, Gamme, Ligne, LigneInput, Lot, NiveauTravaux, Ouvrage, Piece, PieceInput, StatutAffaire, TypePiece,
} from '../api'
import { useSession } from '../ApiContext'
import FormulaireAffaire from '../components/FormulaireAffaire'
import { nomFichier, telecharger } from '../components/telecharger'
import AccesClient from '../components/AccesClient'
import Avenants from '../components/Avenants'
import MargeChantier from '../components/MargeChantier'
import Versions from '../components/Versions'
import Visite from '../components/Visite'
import {
  devinerTypePiece, formatEuros, formatNombre, formatTaux, LIBELLES_GAMME, LIBELLES_NIVEAU, LIBELLES_STATUT, LIBELLES_TYPE_BIEN,
  LIBELLES_TYPE_PIECE, parseNombre, TAUX_TVA, UNITES,
} from '../format'
import Estimation from '../components/Estimation'

type Vue = 'piece' | 'lot'

const GENERAL = ''

export default function AffairePage() {
  const { id = '' } = useParams()
  const { api } = useSession()
  const [affaire, setAffaire] = useState<Affaire | null>(null)
  const [pieces, setPieces] = useState<Piece[]>([])
  const [lots, setLots] = useState<Lot[]>([])
  const [devis, setDevis] = useState<Devis | null>(null)
  const navigate = useNavigate()
  const [vue, setVue] = useState<Vue>('piece')
  const [modification, setModification] = useState(false)
  const [edition, setEdition] = useState<Ligne | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)

  /** Exécute une action et affiche l'erreur éventuelle ; renvoie true si elle a réussi. */
  const run = useCallback(async (action: () => Promise<unknown>) => {
    try {
      await action()
      setError(null)
      return true
    } catch (e) {
      setError((e as Error).message)
      return false
    }
  }, [])

  const reload = useCallback(
    () => run(async () => {
      const [a, p, d] = await Promise.all([api.affaire(id), api.pieces(id), api.devis(id)])
      setAffaire(a)
      setPieces(p)
      setDevis(d)
    }),
    [api, id, run],
  )

  useEffect(() => {
    void reload()
    void run(async () => setLots(await api.lots()))
  }, [api, reload, run])

  if (!affaire || !devis) {
    return error ? <p role="alert" className="erreur">{error}</p> : <p>Chargement…</p>
  }

  const changeStatut = (statut: StatutAffaire) =>
    run(async () => setAffaire(await api.updateAffaire(affaire.id, { ...affaire, statut })))

  /** Le PDF demande le jeton d'accès : on le télécharge puis on le propose comme fichier. */
  async function telechargerPdf(vue: 'PIECE' | 'LOT') {
    if (!affaire) return
    await run(async () => {
      telecharger(await api.devisPdf(affaire.id, vue),
        `devis-${nomFichier(affaire.nom)}-${vue === 'PIECE' ? 'client' : 'chantier'}.pdf`)
    })
  }

  async function supprimer() {
    if (!affaire || !window.confirm(`Supprimer définitivement l'affaire « ${affaire.nom} » et tout son devis ?`)) return
    if (await run(() => api.deleteAffaire(affaire.id))) navigate('/')
  }

  return (
    <>
      <p><Link to="/">← Affaires</Link></p>
      <div className="titre-page">
        <div>
          <h1>{affaire.nom}</h1>
          <p className="sous-titre">
            {[affaire.clientNom, affaire.adresse, LIBELLES_TYPE_BIEN[affaire.typeBien]].filter(Boolean).join(' · ')}
          </p>
        </div>
        <div className="en-ligne">
        <button onClick={() => setModification(true)}>Modifier</button>
        <button className="danger" onClick={() => void supprimer()}>Supprimer</button>
        <label className="en-ligne">
          Statut
          <select value={affaire.statut} onChange={(e) => void changeStatut(e.target.value as StatutAffaire)}>
            {Object.entries(LIBELLES_STATUT).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </label>
        </div>
      </div>

      {modification && (
        <FormulaireAffaire
          affaire={affaire}
          libelle="Modifier l'affaire"
          onCancel={() => setModification(false)}
          onSubmit={(input) => run(async () => {
            setAffaire(await api.updateAffaire(affaire.id, input))
            setModification(false)
          })}
        />
      )}
      {affaire.notes && !modification && <p className="notes">{affaire.notes}</p>}

      {error && <p role="alert" className="erreur">{error}</p>}
      {info && <p role="status" className="info" onClick={() => setInfo(null)}>{info}</p>}

      <Pieces
        pieces={pieces}
        onAdd={(input) => run(async () => {
          await api.createPiece(affaire.id, input)
          await reload()
        })}
        onUpdate={(piece, input) => run(async () => {
          await api.updatePiece(affaire.id, piece.id, { ...input, ordre: piece.ordre })
          await reload()
        })}
        onDelete={(piece) => run(async () => {
          await api.deletePiece(affaire.id, piece.id)
          await reload()
        })}
      />

      <Visite affaire={affaire} pieces={pieces} />

      <Estimation affaireId={affaire.id} version={devis} />

      <section className="carte">
        <div className="titre-section">
          <h2>Devis</h2>
          <div className="en-ligne">
            <button onClick={() => void telechargerPdf('PIECE')}>PDF client (par pièce)</button>
            <button onClick={() => void telechargerPdf('LOT')}>PDF chantier (par lot)</button>
            <button title="Pour Excel ou un autre logiciel (ProGBat…)" onClick={() => void run(async () =>
              telecharger(await api.devisCsv(affaire.id), `devis-${nomFichier(affaire.nom)}.csv`))}>
              Export Excel (CSV)
            </button>
          </div>
          <div className="bascule" role="group" aria-label="Présentation du devis">
            <button aria-pressed={vue === 'piece'} onClick={() => setVue('piece')}>Par pièce</button>
            <button aria-pressed={vue === 'lot'} onClick={() => setVue('lot')}>Par lot</button>
          </div>
        </div>

        {affaire.versionChantierId && (
          <p className="info">Affaire en chantier : le devis est figé (dossier travaux). Les changements passent par un avenant.</p>
        )}
        <TableDevis
          fige={!!affaire.versionChantierId}
          devis={devis}
          pieces={pieces}
          lots={lots}
          vue={vue}
          onEdit={setEdition}
          onChoisirGamme={(pieceId, gamme) => run(async () => {
            await api.choisirGamme(affaire.id, pieceId, gamme)
            await reload()
          })}
          onMove={(ids) => run(async () => {
            await api.reorderLignes(affaire.id, ids)
            await reload()
          })}
          onDelete={(ligne) => run(async () => {
            await api.deleteLigne(affaire.id, ligne.id)
            await reload()
          })}
          onSaveToLibrary={(ligne) => run(async () => {
            const ouvrage = await api.createOuvrage({
              lotId: ligne.lotId, designation: ligne.designation, description: ligne.description,
              unite: ligne.unite, prixUnitaireHt: ligne.prixUnitaireHt, tauxTva: ligne.tauxTva,
              debourseUnitaireHt: ligne.debourseUnitaireHt,
            })
            setInfo(`« ${ouvrage.designation} » ajouté à la bibliothèque.`)
          })}
        />

        {!affaire.versionChantierId && <FormulaireLigne
          key={edition?.id ?? 'nouvelle'}
          ligne={edition}
          pieces={pieces}
          lots={lots}
          onCancel={() => setEdition(null)}
          onSubmit={(input) => run(async () => {
            if (edition) {
              await api.updateLigne(affaire.id, edition.id, input)
            } else {
              await api.createLigne(affaire.id, input)
            }
            setEdition(null)
            await reload()
          })}
          onAddFromLibrary={(ouvrage, pieceId, quantite) => run(async () => {
            await api.createLigneDepuisOuvrage(affaire.id, { ouvrageId: ouvrage.id, pieceId, quantite })
            await reload()
          })}
        />}
      </section>

      <Versions affaire={affaire} onChantier={reload} />

      {affaire.versionChantierId && <Avenants affaire={affaire} pieces={pieces} lots={lots} />}

      <MargeChantier affaireId={affaire.id} lots={lots} version={devis} />

      <AccesClient affaireId={affaire.id} />
    </>
  )
}

function Pieces({ pieces, onAdd, onUpdate, onDelete }: {
  pieces: Piece[]
  onAdd: (input: PieceInput) => Promise<boolean>
  onUpdate: (piece: Piece, input: PieceInput) => Promise<boolean>
  onDelete: (piece: Piece) => Promise<boolean>
}) {
  const [edition, setEdition] = useState<Piece | null>(null)

  return (
    <section className="carte">
      <h2>Pièces</h2>
      <ul className="pastilles">
        {pieces.map((piece) => (
          <li key={piece.id} className={edition?.id === piece.id ? 'active' : undefined}>
            <button className="lien" onClick={() => setEdition(piece)} aria-label={`Modifier ${piece.nom}`}>
              {piece.nom}
              {piece.surfaceM2 != null && <span className="discret"> · {formatNombre(piece.surfaceM2)} m²</span>}
              {piece.surfaceMursM2 != null && (
                <span className="discret"> · murs {formatNombre(piece.surfaceMursM2)} m²</span>
              )}
            </button>
            <button className="icone" aria-label={`Supprimer ${piece.nom}`} onClick={() => void onDelete(piece)}>×</button>
          </li>
        ))}
        {pieces.length === 0 && <li className="discret">Ajoutez les pièces relevées pendant la visite.</li>}
      </ul>
      {edition ? (
        <FormulairePiece
          key={edition.id}
          piece={edition}
          libelle={`Modifier ${edition.nom}`}
          onCancel={() => setEdition(null)}
          onSubmit={async (input) => {
            if (await onUpdate(edition, input)) setEdition(null)
            return true
          }}
        />
      ) : (
        <FormulairePiece libelle="Ajouter une pièce" onSubmit={onAdd} />
      )}
    </section>
  )
}

const enTexte = (valeur: number | null | undefined) => (valeur != null ? String(valeur).replace('.', ',') : '')
const enNombre = (saisie: string) => {
  const valeur = parseNombre(saisie)
  return Number.isNaN(valeur) ? null : valeur
}

function FormulairePiece({ piece, libelle, onSubmit, onCancel }: {
  piece?: Piece
  libelle: string
  onSubmit: (input: PieceInput) => Promise<boolean>
  onCancel?: () => void
}) {
  const [nom, setNom] = useState(piece?.nom ?? '')
  const [surface, setSurface] = useState(enTexte(piece?.surfaceM2))
  const [perimetre, setPerimetre] = useState(enTexte(piece?.perimetreMl))
  const [hauteur, setHauteur] = useState(enTexte(piece?.hauteurM ?? (piece ? null : 2.5)))
  const [typePiece, setTypePiece] = useState<string>(piece?.typePiece ?? '')
  const [niveau, setNiveau] = useState<string>(piece?.niveauTravaux ?? (piece ? '' : 'RENOVATION'))
  const [longueur, setLongueur] = useState('')
  const [largeur, setLargeur] = useState('')

  /** Pièce rectangulaire : longueur × largeur donne la surface et le périmètre. */
  function depuisDimensions(l: string, L: string) {
    const a = enNombre(l)
    const b = enNombre(L)
    if (a != null && b != null) {
      setSurface(enTexte(Math.round(a * b * 100) / 100))
      setPerimetre(enTexte(Math.round(2 * (a + b) * 100) / 100))
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    const ok = await onSubmit({
      nom: nom.trim(), surfaceM2: enNombre(surface), perimetreMl: enNombre(perimetre), hauteurM: enNombre(hauteur),
      typePiece: (typePiece || null) as TypePiece | null, niveauTravaux: (niveau || null) as NiveauTravaux | null,
    })
    if (ok && !piece) {
      setNom('')
      setTypePiece('')
      setSurface('')
      setPerimetre('')
      setLongueur('')
      setLargeur('')
    }
  }

  return (
    <form className="formulaire-piece" onSubmit={submit} aria-label={libelle}>
      <div className="en-ligne">
        <input value={nom} required aria-label="Nom de la pièce" placeholder="Cuisine, SDB, Chambre 1…"
          onChange={(e) => {
            setNom(e.target.value)
            if (!piece) setTypePiece(devinerTypePiece(e.target.value) ?? '')
          }} />
        <select value={typePiece} onChange={(e) => setTypePiece(e.target.value)} aria-label="Type de pièce">
          <option value="">Type…</option>
          {Object.entries(LIBELLES_TYPE_PIECE).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
        </select>
        <select value={niveau} onChange={(e) => setNiveau(e.target.value)} aria-label="Niveau de travaux">
          <option value="">Niveau…</option>
          {Object.entries(LIBELLES_NIVEAU).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
        </select>
        <input value={longueur} onChange={(e) => { setLongueur(e.target.value); depuisDimensions(e.target.value, largeur) }}
          placeholder="Long. m" inputMode="decimal" className="tres-court" aria-label="Longueur en m" />
        <span className="discret">×</span>
        <input value={largeur} onChange={(e) => { setLargeur(e.target.value); depuisDimensions(longueur, e.target.value) }}
          placeholder="Larg. m" inputMode="decimal" className="tres-court" aria-label="Largeur en m" />
        <span className="discret">ou</span>
        <input value={surface} onChange={(e) => setSurface(e.target.value)} placeholder="Surface m²"
          inputMode="decimal" className="court" aria-label="Surface en m²" />
        <input value={perimetre} onChange={(e) => setPerimetre(e.target.value)} placeholder="Périmètre ml"
          inputMode="decimal" className="court" aria-label="Périmètre en ml" />
        <input value={hauteur} onChange={(e) => setHauteur(e.target.value)} placeholder="HSP m"
          inputMode="decimal" className="tres-court" aria-label="Hauteur sous plafond en m" />
        {onCancel && <button type="button" onClick={onCancel}>Annuler</button>}
        <button type="submit">{piece ? 'Enregistrer' : 'Ajouter la pièce'}</button>
      </div>
    </form>
  )
}

/** Quantités proposées à partir des métrés de la pièce. */
export function metres(piece: Piece | undefined): { libelle: string; quantite: number; unite: string }[] {
  if (!piece) return []
  const metres = []
  if (piece.surfaceM2 != null) metres.push({ libelle: 'Sol', quantite: piece.surfaceM2, unite: 'm²' })
  if (piece.surfaceMursM2 != null) metres.push({ libelle: 'Murs', quantite: piece.surfaceMursM2, unite: 'm²' })
  if (piece.surfaceM2 != null) metres.push({ libelle: 'Plafond', quantite: piece.surfaceM2, unite: 'm²' })
  if (piece.perimetreMl != null) metres.push({ libelle: 'Périmètre', quantite: piece.perimetreMl, unite: 'ml' })
  return metres
}

interface Groupe {
  cle: string
  titre: string
  totalHt: number
  lignes: Ligne[]
  /** Vue par pièce seulement : pièce (null = général), gamme retenue et prix de chaque gamme. */
  pieceId?: string | null
  gammeChoisie?: Gamme
  gammes?: Record<Gamme, number> | null
}

/** Regroupe les mêmes lignes par pièce ou par lot : c'est la même donnée vue de deux façons. */
function grouper(devis: Devis, vue: Vue): Groupe[] {
  if (vue === 'piece') {
    return devis.parPiece.map((total) => ({
      cle: total.pieceId ?? GENERAL,
      titre: total.nom,
      totalHt: total.totalHt,
      lignes: devis.lignes.filter((ligne) => ligne.pieceId === total.pieceId),
      pieceId: total.pieceId,
      gammeChoisie: total.gammeChoisie,
      gammes: total.gammes,
    }))
  }
  return devis.parLot.map((total) => ({
    cle: total.lotId,
    titre: `${total.code} · ${total.nom}`,
    totalHt: total.totalHt,
    // Pour le chantier, seules les lignes retenues comptent.
    lignes: devis.lignes.filter((ligne) => ligne.lotId === total.lotId && ligne.incluse),
  }))
}

/** Échange une ligne avec sa voisine dans le groupe affiché, et renvoie le nouvel ordre global des lignes. */
export function deplacer(lignes: Ligne[], groupe: Ligne[], ligne: Ligne, sens: -1 | 1): string[] | null {
  const index = groupe.findIndex((l) => l.id === ligne.id)
  const voisine = groupe[index + sens]
  if (!voisine) return null
  const ids = lignes.map((l) => l.id)
  const a = ids.indexOf(ligne.id)
  const b = ids.indexOf(voisine.id)
  ;[ids[a], ids[b]] = [ids[b], ids[a]]
  return ids
}

function TableDevis({ fige, devis, pieces, lots, vue, onEdit, onMove, onChoisirGamme, onDelete, onSaveToLibrary }: {
  fige: boolean
  devis: Devis
  pieces: Piece[]
  lots: Lot[]
  vue: Vue
  onEdit: (ligne: Ligne) => void
  onMove: (ids: string[]) => Promise<boolean>
  onChoisirGamme: (pieceId: string | null, gamme: Gamme) => Promise<boolean>
  onDelete: (ligne: Ligne) => Promise<boolean>
  onSaveToLibrary: (ligne: Ligne) => Promise<boolean>
}) {
  const nomPiece = (pieceId: string | null) => pieces.find((p) => p.id === pieceId)?.nom ?? 'Général'
  const nomLot = (lotId: string) => {
    const lot = lots.find((l) => l.id === lotId)
    return lot ? `${lot.code} · ${lot.nom}` : ''
  }

  if (devis.lignes.length === 0) {
    return <p className="vide">Aucune ligne. Ajoutez un ouvrage ci-dessous, pièce par pièce.</p>
  }

  return (
    <table className="tableau devis">
      <thead>
        <tr>
          <th>Désignation</th>
          <th>{vue === 'piece' ? 'Lot' : 'Pièce'}</th>
          <th className="nombre">Qté</th>
          <th>Unité</th>
          <th className="nombre">PU HT</th>
          <th className="nombre">TVA</th>
          <th className="nombre">Total HT</th>
          <th><span className="sr-only">Actions</span></th>
        </tr>
      </thead>
      {grouper(devis, vue).map((groupe) => (
        <tbody key={groupe.cle}>
          <tr className="groupe">
            <th colSpan={6}>
              {groupe.titre}
              {groupe.gammes && (
                <span className="gammes" role="group" aria-label={`Gamme ${groupe.titre}`}>
                  {(Object.keys(LIBELLES_GAMME) as Gamme[]).map((gamme) => (
                    <button key={gamme} aria-pressed={groupe.gammeChoisie === gamme} disabled={fige}
                      onClick={() => void onChoisirGamme(groupe.pieceId ?? null, gamme)}>
                      {LIBELLES_GAMME[gamme]} {formatEuros(groupe.gammes![gamme])}
                    </button>
                  ))}
                </span>
              )}
            </th>
            <th className="nombre">{formatEuros(groupe.totalHt)}</th>
            <th />
          </tr>
          {groupe.lignes.map((ligne, index) => (
            <tr key={ligne.id} className={ligne.incluse ? undefined : 'non-incluse'}>
              <td>
                {ligne.gamme && <span className={`badge-gamme gamme-${ligne.gamme}`}>{LIBELLES_GAMME[ligne.gamme]}</span>}
                {ligne.designation}
                {ligne.description && <div className="discret">{ligne.description}</div>}
              </td>
              <td className="discret">{vue === 'piece' ? nomLot(ligne.lotId) : nomPiece(ligne.pieceId)}</td>
              <td className="nombre">{formatNombre(ligne.quantite)}</td>
              <td>{ligne.unite}</td>
              <td className="nombre">{formatEuros(ligne.prixUnitaireHt)}</td>
              <td className="nombre">{formatTaux(ligne.tauxTva)}</td>
              <td className="nombre">{formatEuros(ligne.montantHt)}</td>
              <td className="actions-ligne">
                {fige ? (!ligne.ouvrageId && (
                  <button onClick={() => void onSaveToLibrary(ligne)} title="Réutiliser cet ouvrage dans d'autres devis">
                    → Bibliothèque
                  </button>
                )) : <>
                <button aria-label={`Monter ${ligne.designation}`} disabled={index === 0}
                  onClick={() => { const ids = deplacer(devis.lignes, groupe.lignes, ligne, -1); if (ids) void onMove(ids) }}>↑</button>
                <button aria-label={`Descendre ${ligne.designation}`} disabled={index === groupe.lignes.length - 1}
                  onClick={() => { const ids = deplacer(devis.lignes, groupe.lignes, ligne, 1); if (ids) void onMove(ids) }}>↓</button>
                <button onClick={() => onEdit(ligne)}>Modifier</button>
                {!ligne.ouvrageId && (
                  <button onClick={() => void onSaveToLibrary(ligne)} title="Réutiliser cet ouvrage dans d'autres devis">
                    → Bibliothèque
                  </button>
                )}
                <button className="danger" aria-label={`Supprimer ${ligne.designation}`}
                  onClick={() => void onDelete(ligne)}>Supprimer</button>
                </>}
              </td>
            </tr>
          ))}
        </tbody>
      ))}
      <tfoot>
        {devis.totauxParGamme && (
          <tr className="comparatif">
            <th colSpan={8}>
              Comparatif HT :{' '}
              {(Object.keys(LIBELLES_GAMME) as Gamme[]).map((g) =>
                `tout en ${LIBELLES_GAMME[g]} ${formatEuros(devis.totauxParGamme![g])}`).join(' · ')}
            </th>
          </tr>
        )}
        <tr>
          <th colSpan={6}>Total HT</th>
          <td className="nombre">{formatEuros(devis.totalHt)}</td>
          <td />
        </tr>
        {devis.tva.map((tva) => (
          <tr key={tva.taux}>
            <th colSpan={6}>TVA {formatTaux(tva.taux)} sur {formatEuros(tva.baseHt)}</th>
            <td className="nombre">{formatEuros(tva.montant)}</td>
            <td />
          </tr>
        ))}
        <tr className="total">
          <th colSpan={6}>Total TTC</th>
          <td className="nombre">{formatEuros(devis.totalTtc)}</td>
          <td />
        </tr>
      </tfoot>
    </table>
  )
}

function FormulaireLigne({ ligne, pieces, lots, onSubmit, onCancel, onAddFromLibrary }: {
  ligne: Ligne | null
  pieces: Piece[]
  lots: Lot[]
  onSubmit: (input: LigneInput) => Promise<boolean>
  onCancel: () => void
  onAddFromLibrary: (ouvrage: Ouvrage, pieceId: string | null, quantite: number) => Promise<boolean>
}) {
  const { api } = useSession()
  const [pieceId, setPieceId] = useState(ligne?.pieceId ?? GENERAL)
  const [lotId, setLotId] = useState(ligne?.lotId ?? '')
  const [designation, setDesignation] = useState(ligne?.designation ?? '')
  const [description, setDescription] = useState(ligne?.description ?? '')
  const [unite, setUnite] = useState(ligne?.unite ?? 'U')
  const [quantite, setQuantite] = useState(ligne ? String(ligne.quantite).replace('.', ',') : '')
  const [prix, setPrix] = useState(ligne ? String(ligne.prixUnitaireHt).replace('.', ',') : '')
  const [tauxTva, setTauxTva] = useState(ligne?.tauxTva ?? 10)
  const [gamme, setGamme] = useState<string>(ligne?.gamme ?? '')
  const [debourse, setDebourse] = useState(ligne?.debourseUnitaireHt != null
    ? String(ligne.debourseUnitaireHt).replace('.', ',') : '')
  const [recherche, setRecherche] = useState('')
  const [suggestions, setSuggestions] = useState<Ouvrage[]>([])
  const [invalide, setInvalide] = useState<string | null>(null)

  useEffect(() => {
    if (ligne || recherche.trim().length < 2) {
      setSuggestions([])
      return
    }
    const timer = setTimeout(() => {
      api.ouvrages(recherche.trim()).then(setSuggestions).catch(() => setSuggestions([]))
    }, 250)
    return () => clearTimeout(timer)
  }, [api, ligne, recherche])

  const piece = pieceId === GENERAL ? null : pieceId

  async function submit(event: FormEvent) {
    event.preventDefault()
    const q = parseNombre(quantite)
    const pu = parseNombre(prix)
    const ds = debourse.trim() === '' ? null : parseNombre(debourse)
    if (Number.isNaN(q) || q < 0 || Number.isNaN(pu) || (ds != null && (Number.isNaN(ds) || ds < 0))) {
      setInvalide('Quantité, prix et déboursé doivent être des nombres (quantité et déboursé positifs).')
      return
    }
    setInvalide(null)
    const ok = await onSubmit({
      debourseUnitaireHt: ds,
      pieceId: piece, lotId, designation: designation.trim(), description: description.trim() || null,
      unite, quantite: q, prixUnitaireHt: pu, tauxTva, gamme: (gamme || null) as Gamme | null,
    })
    // On garde la pièce, le lot et la TVA pour enchaîner les lignes d'une même pièce.
    if (ok && !ligne) {
      setDesignation('')
      setDescription('')
      setQuantite('')
      setPrix('')
      setDebourse('')
    }
  }

  async function addFromLibrary(ouvrage: Ouvrage) {
    const q = parseNombre(quantite)
    if (Number.isNaN(q) || q < 0) {
      setInvalide('Indiquez d\'abord la quantité, puis choisissez l\'ouvrage.')
      return
    }
    setInvalide(null)
    if (await onAddFromLibrary(ouvrage, piece, q)) {
      setRecherche('')
      setQuantite('')
    }
  }

  return (
    <form className="formulaire-ligne" onSubmit={submit} aria-label={ligne ? 'Modifier la ligne' : 'Ajouter une ligne'}>
      <h3>{ligne ? 'Modifier la ligne' : 'Ajouter une ligne'}</h3>
      <div className="grille">
        <label>
          Pièce
          <select value={pieceId} onChange={(e) => setPieceId(e.target.value)}>
            <option value={GENERAL}>Général (toute l'affaire)</option>
            {pieces.map((p) => <option key={p.id} value={p.id}>{p.nom}</option>)}
          </select>
        </label>
        <label>
          Quantité
          <input value={quantite} onChange={(e) => setQuantite(e.target.value)} inputMode="decimal" required />
        </label>
        {metres(pieces.find((p) => p.id === pieceId)).length > 0 && (
          <div className="metres" role="group" aria-label="Remplir depuis les métrés">
            {metres(pieces.find((p) => p.id === pieceId)).map((m) => (
              <button type="button" key={m.libelle}
                onClick={() => { setQuantite(enTexte(m.quantite)); setUnite(m.unite) }}>
                {m.libelle} {formatNombre(m.quantite)} {m.unite}
              </button>
            ))}
          </div>
        )}
      </div>

      {!ligne && (
        <div className="bibliotheque">
          <label>
            Depuis la bibliothèque
            <input value={recherche} onChange={(e) => setRecherche(e.target.value)}
              placeholder="Rechercher un ouvrage (carrelage, WC, prise…)" />
          </label>
          {suggestions.length > 0 && (
            <ul className="suggestions">
              {suggestions.map((ouvrage) => (
                <li key={ouvrage.id}>
                  <button type="button" onClick={() => void addFromLibrary(ouvrage)}>
                    {ouvrage.designation}
                    <span className="discret"> · {formatEuros(ouvrage.prixUnitaireHt)} / {ouvrage.unite}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
          <p className="discret">ou saisie libre :</p>
        </div>
      )}

      <div className="grille">
        <label className="large">
          Désignation
          <input value={designation} onChange={(e) => setDesignation(e.target.value)} required />
        </label>
        <label>
          Lot
          <select value={lotId} onChange={(e) => setLotId(e.target.value)} required>
            <option value="" disabled>Choisir…</option>
            {lots.map((lot) => <option key={lot.id} value={lot.id}>{lot.code} · {lot.nom}</option>)}
          </select>
        </label>
        <label>
          Unité
          <select value={unite} onChange={(e) => setUnite(e.target.value)}>
            {UNITES.map((u) => <option key={u} value={u}>{u}</option>)}
          </select>
        </label>
        <label>
          PU HT (€)
          <input value={prix} onChange={(e) => setPrix(e.target.value)} inputMode="decimal" required />
        </label>
        <label>
          TVA
          <select value={tauxTva} onChange={(e) => setTauxTva(Number(e.target.value))}>
            {TAUX_TVA.map((t) => <option key={t} value={t}>{formatTaux(t)}</option>)}
          </select>
        </label>
        <label>
          Déboursé sec unitaire HT (€)
          <input value={debourse} onChange={(e) => setDebourse(e.target.value)} inputMode="decimal"
            placeholder="facultatif" title="Coût de revient : matériaux + main-d'œuvre" />
        </label>
        <label>
          Option
          <select value={gamme} onChange={(e) => setGamme(e.target.value)}>
            <option value="">Commune à toutes les gammes</option>
            {Object.entries(LIBELLES_GAMME).map(([v, l]) => <option key={v} value={v}>Gamme {l}</option>)}
          </select>
        </label>
        <label className="pleine">
          Descriptif (facultatif)
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2}
            placeholder="Matériaux, marque, finitions, mise en œuvre…" />
        </label>
      </div>
      {invalide && <p role="alert" className="erreur">{invalide}</p>}
      <div className="actions">
        {ligne && <button type="button" onClick={onCancel}>Annuler</button>}
        <button type="submit" className="primaire">{ligne ? 'Enregistrer' : 'Ajouter la ligne'}</button>
      </div>
    </form>
  )
}
