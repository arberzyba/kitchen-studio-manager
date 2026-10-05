const TOKEN_KEY = 'token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export async function api<T>(
  path: string,
  options: { method?: string; body?: unknown } = {},
): Promise<T> {
  const token = getToken()
  const response = await fetch(`/api${path}`, {
    method: options.method,
    headers: {
      ...(options.body !== undefined && { 'Content-Type': 'application/json' }),
      ...(token && { Authorization: `Bearer ${token}` }),
    },
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })
  if (!response.ok) {
    // The backend answers errors as problem details with a human-readable "detail"
    const problem = await response.json().catch(() => null)
    throw new ApiError(response.status, problem?.detail ?? 'Request failed')
  }
  return response.json()
}
