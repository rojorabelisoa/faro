/**
 * Réduit une photo de téléphone (souvent 4000 px et plusieurs Mo) à 1600 px de côté en JPEG,
 * pour l'envoyer vite et la stocker sans gaspiller. En cas d'échec, renvoie le fichier d'origine.
 */
export async function reduireImage(fichier: File, cote = 1600, qualite = 0.85): Promise<Blob> {
  try {
    const bitmap = await createImageBitmap(fichier)
    const echelle = Math.min(1, cote / Math.max(bitmap.width, bitmap.height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.round(bitmap.width * echelle)
    canvas.height = Math.round(bitmap.height * echelle)
    canvas.getContext('2d')?.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    bitmap.close()
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', qualite))
    return blob ?? fichier
  } catch {
    return fichier
  }
}
