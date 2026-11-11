/** Propose un fichier (Blob) au téléchargement. */
export function telecharger(blob: Blob, nom: string) {
  const url = URL.createObjectURL(blob)
  const lien = document.createElement('a')
  lien.href = url
  lien.download = nom
  lien.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

/** Nom de fichier lisible à partir d'un libellé (« T3 Lyon » → « T3-Lyon »). */
export const nomFichier = (libelle: string) => libelle.replace(/[^\p{L}\p{N}]+/gu, '-').replace(/^-|-$/g, '')
