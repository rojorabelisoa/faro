import { createContext, useContext } from 'react'
import type { Api, Me } from './api'

export interface Session {
  api: Api
  me: Me
}

export const SessionContext = createContext<Session | null>(null)

export function useSession(): Session {
  const session = useContext(SessionContext)
  if (!session) throw new Error('useSession doit être utilisé sous SessionContext')
  return session
}
