import { useCallback, useEffect, useState, type FormEvent } from 'react'
import type { Lot, LotInput } from '../api'
import { useSession } from '../ApiContext'

/** Lots / corps d'état de l'entreprise : leur ordre est celui du devis présenté par lot. */
export default function LotsPage() {
  const { api } = useSession()
  const [lots, setLots] = useState<Lot[] | null>(null)
  const [edition, setEdition] = useState<Lot | null>(null)
  const [error, setError] = useState<string | null>(null)

  const run = useCallback(async (action: () => Promise<unknown>) => {
    try {
      await action()
      setLots(await api.lots())
      setError(null)
      return true
    } catch (e) {
      setError((e as Error).message)
      return false
    }
  }, [api])

  useEffect(() => {
    void run(async () => undefined)
  }, [run])

  return (
    <>
      <div className="titre-page">
        <h1>Lots</h1>
      </div>
      <p className="sous-titre">Les corps d'état de votre entreprise. Le devis « par lot » suit cet ordre.</p>
      {error && <p role="alert" className="erreur">{error}</p>}

      <FormulaireLot
        key={edition?.id ?? 'nouveau'}
        lot={edition}
        onCancel={() => setEdition(null)}
        onSubmit={async (input) => {
          const ok = await run(() => (edition ? api.updateLot(edition.id, input) : api.createLot(input)))
          if (ok) setEdition(null)
          return ok
        }}
      />

      {lots === null ? (
        <p>Chargement…</p>
      ) : (
        <table className="tableau">
          <thead>
            <tr>
              <th className="nombre">Ordre</th>
              <th>Code</th>
              <th>Nom</th>
              <th><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            {lots.map((lot) => (
              <tr key={lot.id}>
                <td className="nombre">{lot.ordre}</td>
                <td>{lot.code}</td>
                <td>{lot.nom}</td>
                <td className="actions-ligne">
                  <button onClick={() => setEdition(lot)}>Modifier</button>
                  <button className="danger" aria-label={`Supprimer ${lot.nom}`}
                    onClick={() => void run(() => api.deleteLot(lot.id))}>Supprimer</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}

function FormulaireLot({ lot, onSubmit, onCancel }: {
  lot: Lot | null
  onSubmit: (input: LotInput) => Promise<boolean>
  onCancel: () => void
}) {
  const [code, setCode] = useState(lot?.code ?? '')
  const [nom, setNom] = useState(lot?.nom ?? '')
  const [ordre, setOrdre] = useState(lot ? String(lot.ordre) : '')

  async function submit(event: FormEvent) {
    event.preventDefault()
    const position = Number.parseInt(ordre, 10)
    const ok = await onSubmit({ code: code.trim(), nom: nom.trim(), ordre: Number.isNaN(position) ? undefined : position })
    if (ok && !lot) {
      setCode('')
      setNom('')
      setOrdre('')
    }
  }

  return (
    <form className="carte en-ligne" onSubmit={submit} aria-label={lot ? 'Modifier le lot' : 'Nouveau lot'}>
      <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="Code (15)" required
        className="court" aria-label="Code" />
      <input value={nom} onChange={(e) => setNom(e.target.value)} placeholder="Nom du lot (Désamiantage…)" required
        aria-label="Nom" />
      <input value={ordre} onChange={(e) => setOrdre(e.target.value)} placeholder="Ordre" inputMode="numeric"
        className="court" aria-label="Ordre" />
      {lot && <button type="button" onClick={onCancel}>Annuler</button>}
      <button type="submit" className="primaire">{lot ? 'Enregistrer' : 'Ajouter le lot'}</button>
    </form>
  )
}
