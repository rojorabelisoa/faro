import type { ReactNode } from 'react'

/** Icônes au trait (24 × 24), couleur héritée du texte. */
function Icone({ children, taille = 18 }: { children: ReactNode; taille?: number }) {
  return (
    <svg width={taille} height={taille} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{children}</svg>
  )
}

export const IconeAccueil = () => <Icone><rect x="3" y="3" width="7" height="9" rx="2" /><rect x="14" y="3" width="7" height="5" rx="2" /><rect x="14" y="12" width="7" height="9" rx="2" /><rect x="3" y="16" width="7" height="5" rx="2" /></Icone>
export const IconeAffaires = () => <Icone><path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" /></Icone>
export const IconeBibliotheque = () => <Icone><path d="M4 19V5a2 2 0 0 1 2-2h12v16H6a2 2 0 0 0-2 2zM4 19a2 2 0 0 0 2 2h12" /><path d="M9 7h6" /></Icone>
export const IconeLots = () => <Icone><path d="M12 3l9 5-9 5-9-5z" /><path d="M3 13l9 5 9-5" /></Icone>
export const IconeRatios = () => <Icone><path d="M5 19L19 5" /><circle cx="7" cy="7" r="2.5" /><circle cx="17" cy="17" r="2.5" /></Icone>
export const IconeEntreprise = () => <Icone><circle cx="9" cy="8" r="3.5" /><path d="M2.5 20a6.5 6.5 0 0 1 13 0" /><path d="M16 4.5a3.5 3.5 0 0 1 0 7M18 14a6.5 6.5 0 0 1 3.5 6" /></Icone>
export const IconeAdmin = () => <Icone><path d="M12 3l8 3v6c0 4.5-3.4 8.3-8 9-4.6-.7-8-4.5-8-9V6z" /></Icone>
export const IconeSortie = () => <Icone><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" /><path d="M10 17l-5-5 5-5M5 12h11" /></Icone>
export const IconeMenu = () => <Icone taille={20}><path d="M4 6h16M4 12h16M4 18h16" /></Icone>
