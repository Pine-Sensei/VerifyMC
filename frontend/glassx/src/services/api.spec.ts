import { afterEach, describe, expect, it, vi } from 'vitest'

import { apiService } from './api'

describe('api service', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('preserves account selection payloads and allows follow-up retry requests', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({
        success: false,
        code: 'ACCOUNT_SELECTION_REQUIRED',
        message: '请选择账号',
        accounts: [{ username: 'Alice' }, { username: 'Bob' }],
        selectionToken: 'token-1'
      }), { status: 409, headers: { 'Content-Type': 'application/json' } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        success: true,
        message: 'ok'
      }), { status: 200, headers: { 'Content-Type': 'application/json' } }))

    vi.stubGlobal('fetch', fetchMock)

    const firstResponse = await apiService.resetForgotPassword({
      identifier: 'shared@example.com',
      identifierType: 'email',
      code: '123456',
      newPassword: 'Password123',
      language: 'zh'
    })

    expect(firstResponse.code).toBe('ACCOUNT_SELECTION_REQUIRED')
    expect(firstResponse.selectionToken).toBe('token-1')

    const secondResponse = await apiService.resetForgotPassword({
      identifier: 'shared@example.com',
      identifierType: 'email',
      code: '123456',
      newPassword: 'Password123',
      selectedUsername: 'Alice',
      selectionToken: 'token-1',
      language: 'zh'
    })

    expect(secondResponse.success).toBe(true)
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
