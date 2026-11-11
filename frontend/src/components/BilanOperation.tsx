import { useEffect, useState, type FormEvent } from 'react'
import type { Bilan, HypothesesBilan } from '../api'
import { useSession } from '../ApiContext'
import { formatEuros, parseNombre } from '../format'

const CHAMPS: { cle: keyof HypothesesBilan; libelle: string }[] = [
  { cle: 'prixAchat', libelle: "Prix d'achat (€)" },
  { cle: 'fraisAcquisition', libelle: 'Frais de notaire et d’agence (€)' },
  { cle: 'autresFrais', libelle: 'Autres frais (€)' },
  { cle: 'prixRevente', libelle: 'Prix de revente (€)' },
  { cle: 'dureeMois', libelle: 'Durée de l’opération (mois)' },
]
const pourcent = (valeur: number | null) => (valeur == null ? '—' : `${String(valeur).replace('.', ',')} %`)

/** Module investisseur / marchand de biens : rentabilité d'une opération achat – travaux – revente. */
export default function BilanOperation({ affaireId, version }: { affaireId: string; version: unknown }) {
  const { api } = useSession()
  const [bilan, setBilan] = useState<Bilan | null>(null)
  const [saisie, setSaisie] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.bilan(affaireId).then((b) => {
      setBilan(b)
      if (b.hypotheses) {
        setSaisie(Object.fromEntries(CHAMPS.map(({ cle }) => [cle, String(b.hypotheses![cle]).replace('.', ',')])))
      }
    }).catch((e: Error) => setError(e.message))
  }, [api, affaireId, version])

  async function enregistrer(event: FormEvent) {
    event.preventDefault()
    const valeurs = Object.fromEntries(CHAMPS.map(({ cle }) => [cle, parseNombre(saisie[cle] ?? '')]))
    if (Object.values(valeurs).some(Number.isNaN)) {
      setError('Tous les champs doivent être des nombres.')
      return
    }
    try {
      setBilan(await api.enregistrerBilan(affaireId, valeurs as unknown as HypothesesBilan))
      setError(null)
    } catch (e) {
      setError((e as Error).message)
    }
  }

  return (
    <details className="carte" open={!!bilan?.hypotheses}>
      <summary><h2 className="en-ligne-titre">Bilan d'opération (investisseur)</h2></summary>
      <p className="discret">
        Pour un marchand de biens ou un investisseur : les travaux pris en compte sont le montant actuel du devis
        ({bilan ? formatEuros(bilan.travauxTtc) : '…'} TTC). TRI annualisé simplifié, sans financement.
      </p>
      {error && <p role="alert" className="erreur">{error}</p>}
      <form className="grille" onSubmit={enregistrer} aria-label="Hypothèses de l'opération">
        {CHAMPS.map(({ cle, libelle }) => (
          <label key={cle}>{libelle}
            <input value={saisie[cle] ?? ''} inputMode="decimal" required
              onChange={(e) => setSaisie({ ...saisie, [cle]: e.target.value })} />
          </label>
        ))}
        <div className="actions pleine"><button type="submit" className="primaire">Calculer</button></div>
      </form>
      {bilan && bilan.scenarios.length > 0 && (
        <table className="tableau">
          <thead>
            <tr>
              <th>Scénario</th>
              <th className="nombre">Travaux TTC</th>
              <th className="nombre">Coût total</th>
              <th className="nombre">Marge brute</th>
              <th className="nombre">Rentabilité</th>
              <th className="nombre">TRI annuel</th>
            </tr>
          </thead>
          <tbody>
            {bilan.scenarios.map((s) => (
              <tr key={s.scenario}>
                <td>{s.scenario}</td>
                <td className="nombre">{formatEuros(s.travauxTtc)}</td>
                <td className="nombre">{formatEuros(s.coutTotal)}</td>
                <td className={`nombre ${s.margeBrute < 0 ? 'negatif' : ''}`}>{formatEuros(s.margeBrute)}</td>
                <td className="nombre">{pourcent(s.rentabilite)}</td>
                <td className="nombre">{pourcent(s.triAnnuel)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </details>
  )
}
