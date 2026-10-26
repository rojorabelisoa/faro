import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import type { Affaire, Devis, Ligne, LigneInput, Lot, Ouvrage, Piece, StatutAffaire } from '../api'
import { useSession } from '../ApiContext'
import {
  formatEuros, formatNombre, formatTaux, LIBELLES_STATUT, LIBELLES_TYPE_BIEN, parseNombre, TAUX_TVA, UNITES,
} from '../format'

type Vue = 'piece' | 'lot'

const GENERAL = ''

export default function AffairePage() {
  const { id = '' } = useParams()
  const { api } = useSession()
  const [affaire, setAffaire] = useState<Affaire | null>(null)
  const [pieces, setPieces] = useState<Piece[]>([])
  const [lots, setLots] = useState<Lot[]>([])
  const [devis, setDevis] = useState<Devis | null>(null)
  const [vue, setVue] = useState<Vue>('piece')
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
        <label className="en-ligne">
          Statut
          <select value={affaire.statut} onChange={(e) => void changeStatut(e.target.value as StatutAffaire)}>
            {Object.entries(LIBELLES_STATUT).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </label>
      </div>

      {error && <p role="alert" className="erreur">{error}</p>}
      {info && <p role="status" className="info" onClick={() => setInfo(null)}>{info}</p>}

      <Pieces
        pieces={pieces}
        onAdd={(nom, surfaceM2) => run(async () => {
          await api.createPiece(affaire.id, { nom, surfaceM2 })
          await reload()
        })}
        onDelete={(piece) => run(async () => {
          await api.deletePiece(affaire.id, piece.id)
          await reload()
        })}
      />

      <section className="carte">
        <div className="titre-section">
          <h2>Devis</h2>
          <div className="bascule" role="group" aria-label="Présentation du devis">
            <button aria-pressed={vue === 'piece'} onClick={() => setVue('piece')}>Par pièce</button>
            <button aria-pressed={vue === 'lot'} onClick={() => setVue('lot')}>Par lot</button>
          </div>
        </div>

        <TableDevis
          devis={devis}
          pieces={pieces}
          lots={lots}
          vue={vue}
          onEdit={setEdition}
          onDelete={(ligne) => run(async () => {
            await api.deleteLigne(affaire.id, ligne.id)
            await reload()
          })}
          onSaveToLibrary={(ligne) => run(async () => {
            const ouvrage = await api.createOuvrage({
              lotId: ligne.lotId, designation: ligne.designation, description: ligne.description,
              unite: ligne.unite, prixUnitaireHt: ligne.prixUnitaireHt, tauxTva: ligne.tauxTva,
            })
            setInfo(`« ${ouvrage.designation} » ajouté à la bibliothèque.`)
          })}
        />

        <FormulaireLigne
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
        />
      </section>
    </>
  )
}

function Pieces({ pieces, onAdd, onDelete }: {
  pieces: Piece[]
  onAdd: (nom: string, surfaceM2: number | null) => Promise<boolean>
  onDelete: (piece: Piece) => Promise<boolean>
}) {
  const [nom, setNom] = useState('')
  const [surface, setSurface] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault()
    const surfaceM2 = parseNombre(surface)
    if (await onAdd(nom.trim(), Number.isNaN(surfaceM2) ? null : surfaceM2)) {
      setNom('')
      setSurface('')
    }
  }

  return (
    <section className="carte">
      <h2>Pièces</h2>
      <ul className="pastilles">
        {pieces.map((piece) => (
          <li key={piece.id}>
            {piece.nom}
            {piece.surfaceM2 != null && <span className="discret"> · {formatNombre(piece.surfaceM2)} m²</span>}
            <button className="icone" aria-label={`Supprimer ${piece.nom}`} onClick={() => void onDelete(piece)}>×</button>
          </li>
        ))}
        {pieces.length === 0 && <li className="discret">Ajoutez les pièces relevées pendant la visite.</li>}
      </ul>
      <form className="en-ligne" onSubmit={submit} aria-label="Ajouter une pièce">
        <input value={nom} onChange={(e) => setNom(e.target.value)} placeholder="Cuisine, SDB, Chambre 1…" required
          aria-label="Nom de la pièce" />
        <input value={surface} onChange={(e) => setSurface(e.target.value)} placeholder="Surface m²"
          inputMode="decimal" className="court" aria-label="Surface en m²" />
        <button type="submit">Ajouter la pièce</button>
      </form>
    </section>
  )
}

interface Groupe {
  cle: string
  titre: string
  totalHt: number
  lignes: Ligne[]
}

/** Regroupe les mêmes lignes par pièce ou par lot : c'est la même donnée vue de deux façons. */
function grouper(devis: Devis, lots: Lot[], vue: Vue): Groupe[] {
  const ordreLot = new Map(lots.map((lot) => [lot.id, lot.ordre]))
  if (vue === 'piece') {
    return devis.parPiece.map((total) => ({
      cle: total.pieceId ?? GENERAL,
      titre: total.nom,
      totalHt: total.totalHt,
      lignes: devis.lignes
        .filter((ligne) => ligne.pieceId === total.pieceId)
        .sort((a, b) => (ordreLot.get(a.lotId) ?? 0) - (ordreLot.get(b.lotId) ?? 0) || a.ordre - b.ordre),
    }))
  }
  return devis.parLot.map((total) => ({
    cle: total.lotId,
    titre: `${total.code} · ${total.nom}`,
    totalHt: total.totalHt,
    lignes: devis.lignes.filter((ligne) => ligne.lotId === total.lotId),
  }))
}

function TableDevis({ devis, pieces, lots, vue, onEdit, onDelete, onSaveToLibrary }: {
  devis: Devis
  pieces: Piece[]
  lots: Lot[]
  vue: Vue
  onEdit: (ligne: Ligne) => void
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
      {grouper(devis, lots, vue).map((groupe) => (
        <tbody key={groupe.cle}>
          <tr className="groupe">
            <th colSpan={6}>{groupe.titre}</th>
            <th className="nombre">{formatEuros(groupe.totalHt)}</th>
            <th />
          </tr>
          {groupe.lignes.map((ligne) => (
            <tr key={ligne.id}>
              <td>
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
                <button onClick={() => onEdit(ligne)}>Modifier</button>
                {!ligne.ouvrageId && (
                  <button onClick={() => void onSaveToLibrary(ligne)} title="Réutiliser cet ouvrage dans d'autres devis">
                    → Bibliothèque
                  </button>
                )}
                <button className="danger" aria-label={`Supprimer ${ligne.designation}`}
                  onClick={() => void onDelete(ligne)}>Supprimer</button>
              </td>
            </tr>
          ))}
        </tbody>
      ))}
      <tfoot>
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
  const [unite, setUnite] = useState(ligne?.unite ?? 'U')
  const [quantite, setQuantite] = useState(ligne ? String(ligne.quantite).replace('.', ',') : '')
  const [prix, setPrix] = useState(ligne ? String(ligne.prixUnitaireHt).replace('.', ',') : '')
  const [tauxTva, setTauxTva] = useState(ligne?.tauxTva ?? 10)
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
    if (Number.isNaN(q) || q < 0 || Number.isNaN(pu)) {
      setInvalide('Quantité et prix unitaire doivent être des nombres (quantité positive).')
      return
    }
    setInvalide(null)
    const ok = await onSubmit({
      pieceId: piece, lotId, designation: designation.trim(), unite, quantite: q, prixUnitaireHt: pu, tauxTva,
    })
    // On garde la pièce, le lot et la TVA pour enchaîner les lignes d'une même pièce.
    if (ok && !ligne) {
      setDesignation('')
      setQuantite('')
      setPrix('')
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
      </div>
      {invalide && <p role="alert" className="erreur">{invalide}</p>}
      <div className="actions">
        {ligne && <button type="button" onClick={onCancel}>Annuler</button>}
        <button type="submit" className="primaire">{ligne ? 'Enregistrer' : 'Ajouter la ligne'}</button>
      </div>
    </form>
  )
}
