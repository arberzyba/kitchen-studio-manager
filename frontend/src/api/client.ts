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

// Shape of paged list responses from the backend
export type Page<T> = {
  content: T[]
  page: {
    size: number
    number: number
    totalElements: number
    totalPages: number
  }
}

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

type RequestOptions = { method?: string; body?: unknown }

async function request(path: string, options: RequestOptions) {
  const token = getToken()
  const response = await fetch(`/api${path}`, {
    method: options.method,
    headers: {
      ...(options.body !== undefined && { 'Content-Type': 'application/json' }),
      ...(token && { Authorization: `Bearer ${token}` }),
    },
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })
  if (response.status === 401 && token) {
    // The stored token was rejected, so the session has expired. Forget it and start again at the
    // login page; otherwise the dead token would be sent with every request, including the next login.
    setToken(null)
    window.location.assign('/login')
  }
  if (!response.ok) {
    // The backend answers errors as problem details with a human-readable "detail"
    const problem = await response.json().catch(() => null)
    throw new ApiError(response.status, problem?.detail ?? 'Request failed')
  }
  return response
}

export async function api<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const response = await request(path, options)
  // 204 No Content (e.g. after a delete) has no body to parse
  return response.status === 204 ? (undefined as T) : response.json()
}

// For file responses such as PDFs
export async function apiBlob(path: string) {
  return (await request(path, {})).blob()
}
