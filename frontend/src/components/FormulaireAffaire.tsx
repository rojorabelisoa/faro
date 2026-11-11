import type { FormEvent } from 'react'
import type { Affaire, AffaireInput, TypeBien } from '../api'
import { LIBELLES_TYPE_BIEN } from '../format'

/** Formulaire de création ou de modification d'une affaire. */
export default function FormulaireAffaire({ affaire, libelle, onSubmit, onCancel }: {
  affaire?: Affaire
  libelle: string
  onSubmit: (input: AffaireInput) => Promise<unknown>
  onCancel: () => void
}) {
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const texte = (nom: string) => String(form.get(nom) ?? '').trim() || null
    void onSubmit({
      nom: String(form.get('nom')).trim(),
      clientNom: texte('clientNom'),
      clientEmail: texte('clientEmail'),
      clientTelephone: texte('clientTelephone'),
      adresse: texte('adresse'),
      typeBien: form.get('typeBien') as TypeBien,
      statut: affaire?.statut,
      notes: texte('notes'),
    })
  }

  return (
    <form className="carte formulaire" onSubmit={submit} aria-label={libelle}>
      <label>
        Nom de l'affaire
        <input name="nom" required defaultValue={affaire?.nom} placeholder="Rénovation T3 rue Victor Hugo" />
      </label>
      <label>
        Client
        <input name="clientNom" defaultValue={affaire?.clientNom ?? ''} placeholder="M. et Mme Martin" />
      </label>
      <label>
        E-mail du client
        <input name="clientEmail" type="email" defaultValue={affaire?.clientEmail ?? ''} />
      </label>
      <label>
        Téléphone du client
        <input name="clientTelephone" type="tel" defaultValue={affaire?.clientTelephone ?? ''} />
      </label>
      <label>
        Adresse
        <input name="adresse" defaultValue={affaire?.adresse ?? ''} />
      </label>
      <label>
        Type de bien
        <select name="typeBien" defaultValue={affaire?.typeBien ?? 'APPARTEMENT'}>
          {Object.entries(LIBELLES_TYPE_BIEN).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>
      </label>
      <label>
        Notes
        <textarea name="notes" rows={3} defaultValue={affaire?.notes ?? ''} />
      </label>
      <div className="actions">
        <button type="button" onClick={onCancel}>Annuler</button>
        <button type="submit" className="primaire">{affaire ? 'Enregistrer' : 'Créer'}</button>
      </div>
    </form>
  )
}
