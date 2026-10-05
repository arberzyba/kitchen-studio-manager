import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { api, getToken, setToken } from '../api/client'
import type { User } from '../users/types'
import { AuthContext } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [hasToken, setHasToken] = useState(() => getToken() !== null)

  // Loads the logged-in user after a page reload; a rejected (expired) token counts as logged out
  const me = useQuery({
    queryKey: ['me'],
    queryFn: () => api<User>('/auth/me'),
    enabled: hasToken,
    retry: false,
  })

  async function login(email: string, password: string) {
    const response = await api<{ token: string; user: User }>('/auth/login', {
      method: 'POST',
      body: { email, password },
    })
    setToken(response.token)
    queryClient.setQueryData(['me'], response.user)
    setHasToken(true)
  }

  function logout() {
    setToken(null)
    setHasToken(false)
    queryClient.clear()
  }

  return (
    <AuthContext
      value={{
        user: hasToken ? me.data : undefined,
        isLoading: hasToken && me.isPending,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext>
  )
}
