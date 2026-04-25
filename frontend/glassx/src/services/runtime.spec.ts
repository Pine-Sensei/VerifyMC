import { describe, expect, it } from 'vitest'

import { buildApiUrlForBase, buildWebSocketUrlFromApiBase, normalizeApiBase } from './runtime'

describe('runtime service', () => {
  it('strips trailing slashes and /api suffixes from configured api bases', () => {
    expect(normalizeApiBase('https://api.example.com/')).toBe('https://api.example.com')
    expect(normalizeApiBase('https://api.example.com/api')).toBe('https://api.example.com')
  })

  it('builds same-origin api paths when no external base is configured', () => {
    expect(buildApiUrlForBase('', '/config')).toBe('/api/config')
  })

  it('builds absolute api paths for external deployments', () => {
    expect(buildApiUrlForBase('https://api.example.com', '/config')).toBe('https://api.example.com/api/config')
  })

  it('derives websocket urls from the configured api origin', () => {
    expect(buildWebSocketUrlFromApiBase('https://api.example.com', 8081)).toBe('wss://api.example.com:8081')
    expect(buildWebSocketUrlFromApiBase('', 8081, 'http://localhost:3000')).toBe('ws://localhost:8081')
  })
})
