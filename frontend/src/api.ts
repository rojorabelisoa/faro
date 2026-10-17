import { apiUrl } from './config'

export interface Me {
  id: string
  subject: string
  email: string | null
  displayName: string | null
  roles: string[]
}

export async function fetchMe(accessToken: string): Promise<Me> {
  const response = await fetch(`${apiUrl}/api/me`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (!response.ok) {
    throw new Error(`Erreur API ${response.status}`)
  }
  return response.json()
}
