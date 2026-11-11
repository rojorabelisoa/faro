import { describe, expect, it } from 'vitest'
import { devinerTypePiece, parseNombre } from './format'

describe('devinerTypePiece', () => {
  it('reconnaît les noms de pièces courants', () => {
    expect(devinerTypePiece('SDB étage')).toBe('SALLE_DE_BAINS')
    expect(devinerTypePiece("Salle d'eau")).toBe('SALLE_DE_BAINS')
    expect(devinerTypePiece('WC')).toBe('WC')
    expect(devinerTypePiece('Séjour')).toBe('SEJOUR')
    expect(devinerTypePiece('Chambre 2')).toBe('CHAMBRE')
    expect(devinerTypePiece('Dégagement')).toBe('ENTREE_DEGAGEMENT')
    expect(devinerTypePiece('Cuisine ouverte')).toBe('CUISINE')
    expect(devinerTypePiece('Garage')).toBeNull()
  })
})

describe('parseNombre', () => {
  it('accepte la virgule et les espaces', () => {
    expect(parseNombre('1 234,5')).toBe(1234.5)
    expect(parseNombre('')).toBeNaN()
  })
})
