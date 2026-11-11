import { useCallback, useEffect, useState, type FormEvent } from 'react'
import type { ImportResultat, Lot, Ouvrage } from '../api'
import { useSession } from '../ApiContext'
import { formatEuros, formatTaux, parseNombre, TAUX_TVA, UNITES } from '../format'

export default function BibliothequePage() {
  const { api } = useSession()
  const [ouvrages, setOuvrages] = useState<Ouvrage[] | null>(null)
  const [lots, setLots] = useState<Lot[]>([])
  const [recherche, setRecherche] = useState('')
  const [edition, setEdition] = useState<Ouvrage | null>(null)
  const [resultatImport, setResultatImport] = useState<ImportResultat | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(
    (q: string) => api.ouvrages(q).then(setOuvrages).catch((e: Error) => setError(e.message)),
    [api],
  )

  useEffect(() => {
    api.lots().then(setLots).catch((e: Error) => setError(e.message))
  }, [api])

  useEffect(() => {
    const timer = setTimeout(() => void load(recherche.trim()), 250)
    return () => clearTimeout(timer)
  }, [load, recherche])

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    const prix = parseNombre(String(form.get('prix')))
    if (Number.isNaN(prix)) {
      setError('Le prix unitaire doit être un nombre.')
      return
    }
    const input = {
      lotId: String(form.get('lotId')),
      designation: String(form.get('designation')),
      description: String(form.get('description') ?? '').trim() || null,
      unite: String(form.get('unite')),
      prixUnitaireHt: prix,
      tauxTva: Number(form.get('tauxTva')),
    }
    try {
      if (edition) {
        await api.updateOuvrage(edition.id, input)
        setEdition(null)
      } else {
        await api.createOuvrage(input)
        formElement.reset()
      }
      setError(null)
      await load(recherche.trim())
    } catch (e) {
      setError((e as Error).message)
    }
  }

  async function exporter() {
    try {
      const url = URL.createObjectURL(await api.exportOuvrages())
      const lien = document.createElement('a')
      lien.href = url
      lien.download = 'bibliotheque-faro.csv'
      lien.click()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (e) {
      setError((e as Error).message)
    }
  }

  async function importer(fichier: File | undefined) {
    if (!fichier) return
    try {
      const resultat = await api.importOuvrages(fichier)
      setResultatImport(resultat)
      setError(null)
      if (resultat.erreurs.length === 0) await load(recherche.trim())
    } catch (e) {
      setError((e as Error).message)
    }
  }

  async function remove(ouvrage: Ouvrage) {
    try {
      await api.deleteOuvrage(ouvrage.id)
      await load(recherche.trim())
    } catch (e) {
      setError((e as Error).message)
    }
  }

  const nomLot = (lotId: string) => {
    const lot = lots.find((l) => l.id === lotId)
    return lot ? `${lot.code} · ${lot.nom}` : ''
  }

  return (
    <>
      <div className="titre-page">
        <h1>Bibliothèque d'ouvrages</h1>
      </div>
      <p className="sous-titre">Vos prix de référence, réutilisables dans tous les devis.</p>
      {error && <p role="alert" className="erreur">{error}</p>}

      <section className="carte">
        <h3>Importer depuis Excel</h3>
        <p className="discret">
          Enregistrez votre tableau au format « CSV (séparateur : point-virgule) » avec les colonnes
          <code> lot ; designation ; unite ; prix_unitaire_ht ; tva ; description</code>.
          Le lot est son code (07, 10…). Un ouvrage déjà présent (même lot, même désignation) est mis à jour :
          réimporter le fichier suffit pour actualiser les prix. L'export ci-dessous sert de modèle.
        </p>
        <div className="en-ligne">
          <label className="bouton">
            Importer un fichier CSV
            <input type="file" accept=".csv,text/csv" className="sr-only"
              onChange={(e) => { void importer(e.target.files?.[0]); e.target.value = '' }} />
          </label>
          <button onClick={() => void exporter()}>Exporter la bibliothèque (CSV)</button>
        </div>
        {resultatImport && (resultatImport.erreurs.length === 0 ? (
          <p role="status" className="info">
            {resultatImport.importes} ouvrage(s) ajouté(s), {resultatImport.misAJour} mis à jour.
          </p>
        ) : (
          <div role="alert" className="erreur">
            <p>Rien n'a été importé : corrigez ces lignes puis réessayez.</p>
            <ul>
              {resultatImport.erreurs.map((erreur) => (
                <li key={erreur.ligne + erreur.message}>Ligne {erreur.ligne} : {erreur.message}</li>
              ))}
            </ul>
          </div>
        ))}
      </section>

      <form key={edition?.id ?? 'nouveau'} className="carte formulaire-ligne" onSubmit={save}
        aria-label={edition ? "Modifier l'ouvrage" : 'Nouvel ouvrage'}>
        <h3>{edition ? `Modifier « ${edition.designation} »` : 'Nouvel ouvrage'}</h3>
        <div className="grille">
          <label className="large">
            Désignation
            <input name="designation" required defaultValue={edition?.designation} />
          </label>
          <label>
            Lot
            <select name="lotId" required defaultValue={edition?.lotId ?? ''}>
              <option value="" disabled>Choisir…</option>
              {lots.map((lot) => <option key={lot.id} value={lot.id}>{lot.code} · {lot.nom}</option>)}
            </select>
          </label>
          <label>
            Unité
            <select name="unite" defaultValue={edition?.unite ?? 'U'}>
              {UNITES.map((u) => <option key={u} value={u}>{u}</option>)}
            </select>
          </label>
          <label>
            PU HT (€)
            <input name="prix" inputMode="decimal" required
              defaultValue={edition ? String(edition.prixUnitaireHt).replace('.', ',') : ''} />
          </label>
          <label>
            TVA
            <select name="tauxTva" defaultValue={String(edition?.tauxTva ?? 10)}>
              {TAUX_TVA.map((t) => <option key={t} value={t}>{formatTaux(t)}</option>)}
            </select>
          </label>
          <label className="pleine">
            Descriptif
            <textarea name="description" rows={2} defaultValue={edition?.description ?? ''} />
          </label>
        </div>
        <div className="actions">
          {edition && <button type="button" onClick={() => setEdition(null)}>Annuler</button>}
          <button type="submit" className="primaire">{edition ? 'Enregistrer' : 'Ajouter à la bibliothèque'}</button>
        </div>
      </form>

      <input className="recherche" value={recherche} onChange={(e) => setRecherche(e.target.value)}
        placeholder="Rechercher un ouvrage…" aria-label="Rechercher un ouvrage" />

      {ouvrages === null ? (
        <p>Chargement…</p>
      ) : ouvrages.length === 0 ? (
        <p className="vide">Aucun ouvrage. Ajoutez-en ici ou depuis une ligne de devis (« → Bibliothèque »).</p>
      ) : (
        <table className="tableau">
          <thead>
            <tr>
              <th>Désignation</th>
              <th>Lot</th>
              <th>Unité</th>
              <th className="nombre">PU HT</th>
              <th className="nombre">TVA</th>
              <th><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            {ouvrages.map((ouvrage) => (
              <tr key={ouvrage.id}>
                <td>
                  {ouvrage.designation}
                  {ouvrage.description && <div className="discret">{ouvrage.description}</div>}
                </td>
                <td className="discret">{nomLot(ouvrage.lotId)}</td>
                <td>{ouvrage.unite}</td>
                <td className="nombre">{formatEuros(ouvrage.prixUnitaireHt)}</td>
                <td className="nombre">{formatTaux(ouvrage.tauxTva)}</td>
                <td className="actions-ligne">
                  <button onClick={() => setEdition(ouvrage)}>Modifier</button>
                  <button className="danger" onClick={() => void remove(ouvrage)}
                    aria-label={`Supprimer ${ouvrage.designation}`}>Supprimer</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
