const API_PREFIX = '/api'

let runtimeApiBase = normalizeApiBase(import.meta.env.VITE_BOOTSTRAP_API_BASE)

export function normalizeApiBase(value?: string | null): string {
  if (!value) {
    return ''
  }

  let normalized = value.trim()
  if (!normalized) {
    return ''
  }

  normalized = normalized.replace(/\/+$/, '')
  if (normalized.endsWith(API_PREFIX)) {
    normalized = normalized.slice(0, -API_PREFIX.length)
  }

  return normalized
}

export function setRuntimeApiBase(value?: string | null): string {
  runtimeApiBase = normalizeApiBase(value)
  return runtimeApiBase
}

export function getRuntimeApiBase(): string {
  return runtimeApiBase
}

export function buildApiUrlForBase(base: string | undefined | null, endpoint: string): string {
  const normalizedBase = normalizeApiBase(base)
  const normalizedEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`
  return `${normalizedBase}${API_PREFIX}${normalizedEndpoint}`
}

export function buildApiUrl(endpoint: string): string {
  return buildApiUrlForBase(runtimeApiBase, endpoint)
}

export function resolveApiOrigin(apiBase: string | undefined | null, fallbackOrigin?: string): string {
  const normalizedBase = normalizeApiBase(apiBase)
  const originSource = fallbackOrigin ?? (typeof window !== 'undefined' ? window.location.origin : 'http://localhost')

  if (!normalizedBase) {
    return originSource
  }

  return new URL(normalizedBase, originSource).origin
}

export function buildWebSocketUrlFromApiBase(
  apiBase: string | undefined | null,
  wsPort?: number,
  fallbackOrigin?: string,
): string {
  const origin = resolveApiOrigin(apiBase, fallbackOrigin)
  const url = new URL(origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'

  if (typeof wsPort === 'number' && Number.isFinite(wsPort)) {
    url.port = String(wsPort)
  }

  url.pathname = ''
  url.search = ''
  url.hash = ''

  return `${url.protocol}//${url.host}`
}

export function buildRuntimeWebSocketUrl(wsPort?: number, fallbackOrigin?: string): string {
  return buildWebSocketUrlFromApiBase(runtimeApiBase, wsPort, fallbackOrigin)
}
