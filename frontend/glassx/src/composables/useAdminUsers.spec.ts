import { describe, it, expect, beforeEach, vi } from 'vitest'
import { effectScope, ref } from 'vue'
import { useAdminUsers } from './useAdminUsers'

const { mockApiService } = vi.hoisted(() => ({
  mockApiService: {
    getUsersPaginated: vi.fn(),
    getPendingList: vi.fn(),
    getAllUsers: vi.fn(),
  },
}))

vi.mock('@/services/api', () => ({
  apiService: mockApiService,
}))

describe('useAdminUsers', () => {
  const notification = { error: vi.fn() }
  const t = (key: string) => key

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('uses paginated endpoint data when available', async () => {
    mockApiService.getUsersPaginated.mockResolvedValueOnce({
      success: true,
      users: [{ uuid: '1', username: 'alice', email: 'a@a.com' }],
      pagination: {
        currentPage: 2,
        totalCount: 20,
        totalPages: 10,
        hasNext: true,
        hasPrev: true,
      },
    })

    const scope = effectScope()
    const composable = scope.run(() => useAdminUsers({ locale: ref('zh'), t, notification }))!

    await composable.loadAllUsers()

    expect(composable.allUsers.value).toHaveLength(1)
    expect(composable.currentPage.value).toBe(2)
    expect(composable.totalCount.value).toBe(20)
    expect(mockApiService.getPendingList).not.toHaveBeenCalled()
    expect(mockApiService.getAllUsers).not.toHaveBeenCalled()
    scope.stop()
  })

  it('falls back to pending users when paginated endpoint is empty', async () => {
    mockApiService.getUsersPaginated.mockResolvedValueOnce({ success: true, users: [] })
    mockApiService.getPendingList.mockResolvedValueOnce({
      success: true,
      users: [
        { uuid: '2', username: 'bob', email: 'b@a.com' },
        { uuid: '3', username: 'bobby', email: 'bb@a.com' },
      ],
    })

    const scope = effectScope()
    const composable = scope.run(() => useAdminUsers({ locale: ref('zh'), t, notification }))!

    composable.pageSize.value = 1
    await composable.loadAllUsers()

    expect(mockApiService.getPendingList).toHaveBeenCalledTimes(1)
    expect(composable.allUsers.value[0].username).toBe('bob')
    expect(composable.totalCount.value).toBe(2)
    expect(composable.totalPages.value).toBe(2)
    scope.stop()
  })

  it('falls back to all users endpoint when pending users are empty', async () => {
    mockApiService.getUsersPaginated.mockResolvedValueOnce({ success: true, users: [] })
    mockApiService.getPendingList.mockResolvedValueOnce({ success: true, users: [] })
    mockApiService.getAllUsers.mockResolvedValueOnce({
      success: true,
      users: [{ uuid: '4', username: 'carol', email: 'c@a.com' }],
    })

    const scope = effectScope()
    const composable = scope.run(() => useAdminUsers({ locale: ref('zh'), t, notification }))!

    await composable.loadAllUsers()

    expect(mockApiService.getAllUsers).toHaveBeenCalledTimes(1)
    expect(composable.allUsers.value[0].username).toBe('carol')
    expect(composable.totalCount.value).toBe(1)
    scope.stop()
  })
})
