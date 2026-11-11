import { useCallback, useEffect, useState, type FormEvent } from 'react'
import type { Affaire, PhotoInfo, Piece, Visite as VisiteData } from '../api'
import { useSession } from '../ApiContext'
import { reduireImage } from './reduireImage'
import { nomFichier, telecharger } from './telecharger'

const GENERAL = ''

/** Compte-rendu de visite : notes et photos par pièce, prises sur place (téléphone). */
export default function Visite({ affaire, pieces }: { affaire: Affaire; pieces: Piece[] }) {
  const { api } = useSession()
  const [visite, setVisite] = useState<VisiteData | null>(null)
  const [pieceId, setPieceId] = useState(GENERAL)
  const [texte, setTexte] = useState('')
  const [legende, setLegende] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const run = useCallback(async (action: () => Promise<unknown>) => {
    try {
      await action()
      setVisite(await api.visite(affaire.id))
      setError(null)
      return true
    } catch (e) {
      setError((e as Error).message)
      return false
    }
  }, [api, affaire.id])

  useEffect(() => {
    void run(async () => undefined)
  }, [run])

  const piece = pieceId === GENERAL ? null : pieceId

  async function ajouterNote(event: FormEvent) {
    event.preventDefault()
    await run(async () => {
      await api.addObservation(affaire.id, { pieceId: piece, texte })
      setTexte('')
    })
  }

  async function ajouterPhotos(fichiers: FileList | null) {
    if (!fichiers?.length) return
    setEnvoi(true)
    await run(async () => {
      for (const fichier of Array.from(fichiers)) {
        await api.addPhoto(affaire.id, await reduireImage(fichier), piece, legende.trim())
      }
      setLegende('')
    })
    setEnvoi(false)
  }

  const sections = [{ id: null as string | null, nom: 'Généralités' }, ...pieces.map((p) => ({ id: p.id as string | null, nom: p.nom }))]

  return (
    <section className="carte" aria-label="Visite">
      <div className="titre-section">
        <h2>Visite</h2>
        <button onClick={() => void run(async () =>
          telecharger(await api.visitePdf(affaire.id), `compte-rendu-${nomFichier(affaire.nom)}.pdf`))}>
          PDF compte-rendu
        </button>
      </div>
      {error && <p role="alert" className="erreur">{error}</p>}

      {visite && sections.map((section) => {
        const notes = visite.observations.filter((o) => o.pieceId === section.id)
        const photos = visite.photos.filter((p) => p.pieceId === section.id)
        if (notes.length === 0 && photos.length === 0) return null
        return (
          <div key={section.id ?? GENERAL} className="section-visite">
            <h3>{section.nom}</h3>
            <ul className="notes-visite">
              {notes.map((note) => (
                <li key={note.id}>
                  {note.texte}
                  <button className="icone" aria-label="Supprimer la note"
                    onClick={() => void run(() => api.deleteObservation(affaire.id, note.id))}>×</button>
                </li>
              ))}
            </ul>
            {photos.length > 0 && (
              <div className="photos">
                {photos.map((photo) => (
                  <Vignette key={photo.id} affaireId={affaire.id} photo={photo}
                    onDelete={() => void run(() => api.deletePhoto(affaire.id, photo.id))} />
                ))}
              </div>
            )}
          </div>
        )
      })}
      {visite && visite.observations.length === 0 && visite.photos.length === 0 && (
        <p className="vide">Notez ici ce que vous relevez sur place, pièce par pièce, avec des photos.</p>
      )}

      <form className="formulaire-ligne" onSubmit={ajouterNote} aria-label="Ajouter une note de visite">
        <div className="grille">
          <label>
            Pièce concernée
            <select value={pieceId} onChange={(e) => setPieceId(e.target.value)}>
              <option value={GENERAL}>Généralités</option>
              {pieces.map((p) => <option key={p.id} value={p.id}>{p.nom}</option>)}
            </select>
          </label>
          <label className="pleine">
            Note
            <textarea value={texte} onChange={(e) => setTexte(e.target.value)} rows={2} required
              placeholder="État existant, contraintes d'accès, souhaits du client…" />
          </label>
          <label className="large">
            Légende des photos (facultatif)
            <input value={legende} onChange={(e) => setLegende(e.target.value)} />
          </label>
        </div>
        <div className="actions">
          <label className="bouton">
            {envoi ? 'Envoi…' : 'Ajouter des photos'}
            <input type="file" accept="image/*" capture="environment" multiple className="sr-only" disabled={envoi}
              onChange={(e) => { void ajouterPhotos(e.target.files); e.target.value = '' }} />
          </label>
          <button type="submit" className="primaire">Ajouter la note</button>
        </div>
      </form>
    </section>
  )
}

/** Les photos demandent le jeton : on les charge en Blob pour les afficher. */
function Vignette({ affaireId, photo, onDelete }: { affaireId: string; photo: PhotoInfo; onDelete: () => void }) {
  const { api } = useSession()
  const [url, setUrl] = useState<string | null>(null)

  useEffect(() => {
    let objectUrl: string | null = null
    api.photo(affaireId, photo.id)
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob)
        setUrl(objectUrl)
      })
      .catch(() => setUrl(null))
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [api, affaireId, photo.id])

  return (
    <figure className="vignette">
      {url ? (
        <a href={url} target="_blank" rel="noreferrer"><img src={url} alt={photo.legende ?? 'Photo de visite'} /></a>
      ) : <div className="vignette-vide" />}
      <figcaption>
        {photo.legende}
        <button className="icone" aria-label="Supprimer la photo" onClick={onDelete}>×</button>
      </figcaption>
    </figure>
  )
}
